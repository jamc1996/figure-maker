package com.figuremaker;

import com.google.gson.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FigureCanvas extends JPanel {
    private List<CanvasElement> elements;
    private CanvasElement selectedElement;
    private Point dragStart;
    private Point elementDragStart;
    private boolean isDragging;
    private boolean isResizing;
    private int resizeHandle; // -1=none, 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
    private int startWidth;
    private int startHeight;
    private double scale = 1.0;
    private static final double ZOOM_STEP = 1.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;
    
    public FigureCanvas() {
        elements = new ArrayList<>();
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
        setLayout(null);
        setFocusable(true);
        setupMouseListeners();
    }
    
    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClicked(e);
            }
        };
        
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Zoom when Ctrl or Meta is held (Ctrl on Windows/Linux, Cmd on macOS)
                if (e.isControlDown() || e.isMetaDown()) {
                    int notches = e.getWheelRotation();
                    if (notches < 0) {
                        zoomIn();
                    } else {
                        zoomOut();
                    }
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                char c = e.getKeyChar();

                // Delete selected element with Backspace/Delete, but don't intercept when editing text
                if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) {
                    if (selectedElement instanceof TextElement) {
                        TextElement te = (TextElement) selectedElement;
                        if (te.isEditing()) return;
                    }

                    if (selectedElement != null) {
                        elements.remove(selectedElement);
                        selectedElement = null;
                        repaint();
                    }
                    return;
                }

                if (c == '+') {
                    zoomIn();
                } else if (c == '-') {
                    zoomOut();
                } else if (c == '0') {
                    resetZoom();
                }
            }
        });
        
        // Add mouse motion listener for cursor changes
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getX(), e.getY());
            }
        });
    }
    
    private void updateCursor(int x, int y) {
        // Convert screen coords to logical (canvas) coords
        int lx = (int) (x / scale);
        int ly = (int) (y / scale);

        if (selectedElement instanceof ImageElement) {
            ImageElement imageElement = (ImageElement) selectedElement;
            int handle = imageElement.getResizeHandleAt(lx, ly);

            switch (handle) {
                case 0: // Top-left
                case 3: // Bottom-right
                    setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    break;
                case 1: // Top-right
                case 2: // Bottom-left
                    setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    break;
                default:
                    setCursor(Cursor.getDefaultCursor());
                    break;
            }
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }
    
    private void handleMousePressed(MouseEvent e) {
        requestFocusInWindow();
        // Stop editing any text element
        for (CanvasElement element : elements) {
            if (element instanceof TextElement) {
                TextElement textElement = (TextElement) element;
                if (textElement.isEditing()) {
                    textElement.stopEditing(this);
                }
            }
        }
        
        // Check if clicking on a resize handle of the selected element
        if (selectedElement instanceof ImageElement) {
            ImageElement imageElement = (ImageElement) selectedElement;
            int lx = (int) (e.getX() / scale);
            int ly = (int) (e.getY() / scale);
            resizeHandle = imageElement.getResizeHandleAt(lx, ly);

            if (resizeHandle >= 0) {
                isResizing = true;
                isDragging = false;
                dragStart = new Point(lx, ly);
                elementDragStart = new Point(selectedElement.getX(), selectedElement.getY());
                startWidth = selectedElement.getWidth();
                startHeight = selectedElement.getHeight();
                return;
            }
        }
        
        // Find element at click position (convert to logical coords)
        int lx = (int) (e.getX() / scale);
        int ly = (int) (e.getY() / scale);
        CanvasElement clickedElement = findElementAt(lx, ly);
        
        if (clickedElement != null) {
            if (selectedElement != null) {
                selectedElement.setSelected(false);
            }
            selectedElement = clickedElement;
            selectedElement.setSelected(true);
            
            dragStart = new Point(lx, ly);
            elementDragStart = new Point(selectedElement.getX(), selectedElement.getY());
            isDragging = false;
            isResizing = false;
        } else {
            if (selectedElement != null) {
                selectedElement.setSelected(false);
                selectedElement = null;
            }
        }
        
        // Show popup menu on right-click
        if (e.getButton() == MouseEvent.BUTTON3 && selectedElement != null) {
            showContextMenu(e.getX(), e.getY());
        }
        
        repaint();
    }
    
    private void showContextMenu(int x, int y) {
        if (selectedElement == null) return;
        
        JPopupMenu popup = new JPopupMenu();
        
        // Always show set position option
        JMenuItem setPositionItem = new JMenuItem("Set Position...");
        setPositionItem.addActionListener(e -> showPositionDialog());
        popup.add(setPositionItem);
        
        // If it's a clipping mask, add release option
        if (selectedElement instanceof GroupElement) {
            GroupElement group = (GroupElement) selectedElement;
            if (group.isClippingMask()) {
                popup.addSeparator();
                JMenuItem releaseClipItem = new JMenuItem("Release Clipping Mask");
                releaseClipItem.addActionListener(e -> {
                    group.releaseClippingMask();
                    repaint();
                });
                popup.add(releaseClipItem);
            }
            
            // Add ungroup option
            popup.addSeparator();
            JMenuItem ungroupItem = new JMenuItem("Ungroup");
            ungroupItem.addActionListener(e -> ungroupSelected());
            popup.add(ungroupItem);
        }
        
        popup.show(this, x, y);
    }
    
    private void ungroupSelected() {
        if (!(selectedElement instanceof GroupElement)) return;
        
        GroupElement group = (GroupElement) selectedElement;
        List<CanvasElement> children = group.getChildren();
        
        // Remove the group
        elements.remove(group);
        
        // Add all children to the canvas
        for (CanvasElement child : children) {
            elements.add(child);
        }
        
        selectedElement = null;
        repaint();
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (selectedElement != null && dragStart != null) {
            int lx = (int) (e.getX() / scale);
            int ly = (int) (e.getY() / scale);

            if (isResizing) {
                int dx = lx - dragStart.x;
                int dy = ly - dragStart.y;

                int newX = elementDragStart.x;
                int newY = elementDragStart.y;
                int newWidth = startWidth;
                int newHeight = startHeight;

                // Calculate aspect ratio if Shift is held
                boolean preserveAspect = e.isShiftDown();
                double aspectRatio = (double) startWidth / startHeight;

                switch (resizeHandle) {
                    case 0: // Top-left
                        newWidth = startWidth - dx;
                        newHeight = startHeight - dy;
                        if (preserveAspect) {
                            if (Math.abs(dx) > Math.abs(dy)) {
                                newHeight = (int) (newWidth / aspectRatio);
                                dy = startHeight - newHeight;
                            } else {
                                newWidth = (int) (newHeight * aspectRatio);
                                dx = startWidth - newWidth;
                            }
                        }
                        newX = elementDragStart.x + dx;
                        newY = elementDragStart.y + dy;
                        break;
                    case 1: // Top-right
                        newWidth = startWidth + dx;
                        newHeight = startHeight - dy;
                        if (preserveAspect) {
                            if (Math.abs(dx) > Math.abs(dy)) {
                                newHeight = (int) (newWidth / aspectRatio);
                                dy = startHeight - newHeight;
                            } else {
                                newWidth = (int) (newHeight * aspectRatio);
                            }
                        }
                        newY = elementDragStart.y + dy;
                        break;
                    case 2: // Bottom-left
                        newWidth = startWidth - dx;
                        newHeight = startHeight + dy;
                        if (preserveAspect) {
                            if (Math.abs(dx) > Math.abs(dy)) {
                                newHeight = (int) (newWidth / aspectRatio);
                            } else {
                                newWidth = (int) (newHeight * aspectRatio);
                                dx = startWidth - newWidth;
                            }
                        }
                        newX = elementDragStart.x + dx;
                        break;
                    case 3: // Bottom-right
                        newWidth = startWidth + dx;
                        newHeight = startHeight + dy;
                        if (preserveAspect) {
                            if (Math.abs(dx) > Math.abs(dy)) {
                                newHeight = (int) (newWidth / aspectRatio);
                            } else {
                                newWidth = (int) (newHeight * aspectRatio);
                            }
                        }
                        break;
                }

                // Enforce minimum size
                if (newWidth > 20 && newHeight > 20) {
                    selectedElement.setPosition(newX, newY);
                    selectedElement.setSize(newWidth, newHeight);
                }

                repaint();
            } else {
                isDragging = true;
                int dx = lx - dragStart.x;
                int dy = ly - dragStart.y;

                selectedElement.setPosition(elementDragStart.x + dx, elementDragStart.y + dy);
                repaint();
            }
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        isDragging = false;
        isResizing = false;
        resizeHandle = -1;
        dragStart = null;
        elementDragStart = null;
    }
    
    private void handleMouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && selectedElement instanceof TextElement) {
            TextElement textElement = (TextElement) selectedElement;
            textElement.startEditing(this);
        }
    }
    
    private CanvasElement findElementAt(int x, int y) {
        // Search in reverse order to get topmost element
        for (int i = elements.size() - 1; i >= 0; i--) {
            CanvasElement element = elements.get(i);
            if (element.contains(x, y)) {
                return element;
            }
        }
        return null;
    }
    
    private void showPositionDialog() {
        if (selectedElement == null) return;
        
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField xField = new JTextField(String.valueOf(selectedElement.getX()));
        JTextField yField = new JTextField(String.valueOf(selectedElement.getY()));
        
        panel.add(new JLabel("X (from left):"));
        panel.add(xField);
        panel.add(new JLabel("Y (from top):"));
        panel.add(yField);
        
        int result = JOptionPane.showConfirmDialog(
            this, panel, "Set Position", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                int x = Integer.parseInt(xField.getText());
                int y = Integer.parseInt(yField.getText());
                selectedElement.setPosition(x, y);
                repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid position values. Please enter numbers.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public void addImage(File imageFile) {
        try {
            ImageElement imageElement = new ImageElement(imageFile, 50, 50);
            elements.add(imageElement);
            repaint();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading image: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void addTextBox() {
        TextElement textElement = new TextElement(50, 50);
        elements.add(textElement);
        repaint();
    }
    
    public void importSVG(File svgFile) throws Exception {
        List<CanvasElement> svgElements = SVGParser.parseSVG(svgFile);
        elements.addAll(svgElements);
        repaint();
    }
    
    public void clear() {
        elements.clear();
        if (selectedElement != null) {
            selectedElement = null;
        }
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        java.awt.geom.AffineTransform at = g2.getTransform();
        g2.scale(scale, scale);

        for (CanvasElement element : elements) {
            element.draw(g2);
        }

        g2.setTransform(at);
        g2.dispose();
    }

    public void zoomIn() {
        setScale(scale * ZOOM_STEP);
    }

    public void zoomOut() {
        setScale(scale / ZOOM_STEP);
    }

    public void resetZoom() {
        setScale(1.0);
    }

    public void setScale(double s) {
        double newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        if (Math.abs(newScale - this.scale) < 1e-6) return;
        this.scale = newScale;
        revalidate();
        repaint();
    }
    
    public void saveToFile(File file) throws IOException {
        JsonArray jsonElements = new JsonArray();
        
        for (CanvasElement element : elements) {
            JsonObject jsonElement = new JsonObject();
            jsonElement.addProperty("type", element.getType());
            jsonElement.addProperty("x", element.getX());
            jsonElement.addProperty("y", element.getY());
            jsonElement.addProperty("width", element.getWidth());
            jsonElement.addProperty("height", element.getHeight());
            
            if (element instanceof ImageElement) {
                ImageElement imageElement = (ImageElement) element;
                jsonElement.addProperty("imagePath", imageElement.getImagePath());
                jsonElement.addProperty("imageData", imageElement.getImageAsBase64());
            } else if (element instanceof SVGTextElement) {
                SVGTextElement svgTextElement = (SVGTextElement) element;
                jsonElement.addProperty("text", svgTextElement.getText());
                jsonElement.addProperty("fontName", svgTextElement.getFont().getName());
                jsonElement.addProperty("fontSize", svgTextElement.getFont().getSize());
                jsonElement.addProperty("fontStyle", svgTextElement.getFont().getStyle());
                jsonElement.addProperty("textColor", colorToString(svgTextElement.getTextColor()));
                jsonElement.addProperty("rotation", svgTextElement.getRotation());
            } else if (element instanceof TextElement) {
                TextElement textElement = (TextElement) element;
                jsonElement.addProperty("text", textElement.getText());
                jsonElement.addProperty("fontName", textElement.getFont().getName());
                jsonElement.addProperty("fontSize", textElement.getFont().getSize());
                jsonElement.addProperty("fontStyle", textElement.getFont().getStyle());
            } else if (element instanceof RectElement) {
                RectElement rectElement = (RectElement) element;
                jsonElement.addProperty("fillColor", colorToString(rectElement.getFillColor()));
                jsonElement.addProperty("strokeColor", colorToString(rectElement.getStrokeColor()));
                jsonElement.addProperty("strokeWidth", rectElement.getStrokeWidth());
            } else if (element instanceof CircleElement) {
                CircleElement circleElement = (CircleElement) element;
                jsonElement.addProperty("fillColor", colorToString(circleElement.getFillColor()));
                jsonElement.addProperty("strokeColor", colorToString(circleElement.getStrokeColor()));
                jsonElement.addProperty("strokeWidth", circleElement.getStrokeWidth());
            } else if (element instanceof PathElement) {
                PathElement pathElement = (PathElement) element;
                jsonElement.addProperty("fillColor", colorToString(pathElement.getFillColor()));
                jsonElement.addProperty("strokeColor", colorToString(pathElement.getStrokeColor()));
                jsonElement.addProperty("strokeWidth", pathElement.getStrokeWidth());
                jsonElement.addProperty("pathData", pathToString(pathElement.getPath()));
            } else if (element instanceof GroupElement) {
                GroupElement groupElement = (GroupElement) element;
                jsonElement.addProperty("groupId", groupElement.getGroupId());
                jsonElement.addProperty("isClippingMask", groupElement.isClippingMask());
                
                // Serialize children
                JsonArray childrenArray = new JsonArray();
                for (CanvasElement child : groupElement.getChildren()) {
                    JsonObject childJson = new JsonObject();
                    childJson.addProperty("type", child.getType());
                    childJson.addProperty("x", child.getX());
                    childJson.addProperty("y", child.getY());
                    childJson.addProperty("width", child.getWidth());
                    childJson.addProperty("height", child.getHeight());
                    
                    // Add type-specific properties
                    if (child instanceof RectElement) {
                        RectElement rect = (RectElement) child;
                        childJson.addProperty("fillColor", colorToString(rect.getFillColor()));
                        childJson.addProperty("strokeColor", colorToString(rect.getStrokeColor()));
                        childJson.addProperty("strokeWidth", rect.getStrokeWidth());
                    } else if (child instanceof CircleElement) {
                        CircleElement circle = (CircleElement) child;
                        childJson.addProperty("fillColor", colorToString(circle.getFillColor()));
                        childJson.addProperty("strokeColor", colorToString(circle.getStrokeColor()));
                        childJson.addProperty("strokeWidth", circle.getStrokeWidth());
                    } else if (child instanceof PathElement) {
                        PathElement path = (PathElement) child;
                        childJson.addProperty("fillColor", colorToString(path.getFillColor()));
                        childJson.addProperty("strokeColor", colorToString(path.getStrokeColor()));
                        childJson.addProperty("strokeWidth", path.getStrokeWidth());
                        childJson.addProperty("pathData", pathToString(path.getPath()));
                    } else if (child instanceof SVGTextElement) {
                        SVGTextElement svgText = (SVGTextElement) child;
                        childJson.addProperty("text", svgText.getText());
                        childJson.addProperty("fontName", svgText.getFont().getName());
                        childJson.addProperty("fontSize", svgText.getFont().getSize());
                        childJson.addProperty("fontStyle", svgText.getFont().getStyle());
                        childJson.addProperty("textColor", colorToString(svgText.getTextColor()));
                        childJson.addProperty("rotation", svgText.getRotation());
                    } else if (child instanceof TextElement) {
                        TextElement text = (TextElement) child;
                        childJson.addProperty("text", text.getText());
                        childJson.addProperty("fontName", text.getFont().getName());
                        childJson.addProperty("fontSize", text.getFont().getSize());
                        childJson.addProperty("fontStyle", text.getFont().getStyle());
                        if (child instanceof TextElementWithColor) {
                            TextElementWithColor textColor = (TextElementWithColor) child;
                            childJson.addProperty("textColor", colorToString(textColor.getTextColor()));
                        }
                    }
                    
                    childrenArray.add(childJson);
                }
                jsonElement.add("children", childrenArray);
            }
            
            jsonElements.add(jsonElement);
        }
        
        JsonObject root = new JsonObject();
        root.add("elements", jsonElements);
        
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(root, writer);
        }
    }
    
    public void loadFromFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            JsonArray jsonElements = root.getAsJsonArray("elements");
            
            elements.clear();
            
            for (int i = 0; i < jsonElements.size(); i++) {
                JsonObject jsonElement = jsonElements.get(i).getAsJsonObject();
                String type = jsonElement.get("type").getAsString();
                int x = jsonElement.get("x").getAsInt();
                int y = jsonElement.get("y").getAsInt();
                int width = jsonElement.get("width").getAsInt();
                int height = jsonElement.get("height").getAsInt();
                
                if (type.equals("image")) {
                    String imagePath = jsonElement.get("imagePath").getAsString();
                    String imageData = jsonElement.get("imageData").getAsString();
                    
                    try {
                        java.awt.image.BufferedImage image = ImageElement.decodeBase64Image(imageData);
                        ImageElement imageElement = new ImageElement(image, x, y, width, height, imagePath);
                        elements.add(imageElement);
                    } catch (Exception ex) {
                        System.err.println("Error loading image: " + ex.getMessage());
                    }
                } else if (type.equals("text")) {
                    String text = jsonElement.get("text").getAsString();
                    String fontName = jsonElement.get("fontName").getAsString();
                    int fontSize = jsonElement.get("fontSize").getAsInt();
                    int fontStyle = jsonElement.get("fontStyle").getAsInt();
                    Font font = new Font(fontName, fontStyle, fontSize);
                    
                    TextElement textElement = new TextElement(x, y, width, height, text, font);
                    elements.add(textElement);
                } else if (type.equals("svg-text")) {
                    String text = jsonElement.get("text").getAsString();
                    String fontName = jsonElement.get("fontName").getAsString();
                    int fontSize = jsonElement.get("fontSize").getAsInt();
                    int fontStyle = jsonElement.get("fontStyle").getAsInt();
                    Font font = new Font(fontName, fontStyle, fontSize);
                    Color textColor = stringToColor(jsonElement.get("textColor").getAsString());
                    double rotation = jsonElement.has("rotation") ? jsonElement.get("rotation").getAsDouble() : 0.0;
                    
                    SVGTextElement svgTextElement = new SVGTextElement(x, y, width, height, text, font, textColor, rotation);
                    elements.add(svgTextElement);
                } else if (type.equals("rect")) {
                    Color fillColor = stringToColor(jsonElement.get("fillColor").getAsString());
                    Color strokeColor = stringToColor(jsonElement.get("strokeColor").getAsString());
                    float strokeWidth = jsonElement.get("strokeWidth").getAsFloat();
                    
                    RectElement rectElement = new RectElement(x, y, width, height, fillColor, strokeColor, strokeWidth);
                    elements.add(rectElement);
                } else if (type.equals("circle")) {
                    Color fillColor = stringToColor(jsonElement.get("fillColor").getAsString());
                    Color strokeColor = stringToColor(jsonElement.get("strokeColor").getAsString());
                    float strokeWidth = jsonElement.get("strokeWidth").getAsFloat();
                    
                    CircleElement circleElement = new CircleElement(x, y, width, height, fillColor, strokeColor, strokeWidth);
                    elements.add(circleElement);
                } else if (type.equals("path")) {
                    Color fillColor = stringToColor(jsonElement.get("fillColor").getAsString());
                    Color strokeColor = stringToColor(jsonElement.get("strokeColor").getAsString());
                    float strokeWidth = jsonElement.get("strokeWidth").getAsFloat();
                    String pathData = jsonElement.get("pathData").getAsString();
                    
                    java.awt.geom.Path2D.Double path = stringToPath(pathData);
                    PathElement pathElement = new PathElement(path, x, y, width, height, fillColor, strokeColor, strokeWidth);
                    elements.add(pathElement);
                } else if (type.equals("group") || type.equals("clipping-mask")) {
                    String groupId = jsonElement.has("groupId") ? jsonElement.get("groupId").getAsString() : null;
                    boolean isClippingMask = jsonElement.has("isClippingMask") && jsonElement.get("isClippingMask").getAsBoolean();
                    
                    GroupElement group = new GroupElement(x, y, width, height, groupId);
                    group.setClippingMask(isClippingMask);
                    
                    // Load children
                    if (jsonElement.has("children")) {
                        JsonArray childrenArray = jsonElement.getAsJsonArray("children");
                        for (int j = 0; j < childrenArray.size(); j++) {
                            JsonObject childJson = childrenArray.get(j).getAsJsonObject();
                            CanvasElement child = loadElementFromJson(childJson);
                            if (child != null) {
                                group.addChild(child);
                            }
                        }
                    }
                    
                    elements.add(group);
                }
            }
            
            repaint();
        }
    }
    
    private CanvasElement loadElementFromJson(JsonObject jsonElement) {
        String type = jsonElement.get("type").getAsString();
        int x = jsonElement.get("x").getAsInt();
        int y = jsonElement.get("y").getAsInt();
        int width = jsonElement.get("width").getAsInt();
        int height = jsonElement.get("height").getAsInt();
        
        if (type.equals("rect")) {
            Color fillColor = stringToColor(jsonElement.get("fillColor").getAsString());
            Color strokeColor = stringToColor(jsonElement.get("strokeColor").getAsString());
            float strokeWidth = jsonElement.get("strokeWidth").getAsFloat();
            return new RectElement(x, y, width, height, fillColor, strokeColor, strokeWidth);
        } else if (type.equals("circle")) {
            Color fillColor = stringToColor(jsonElement.get("fillColor").getAsString());
            Color strokeColor = stringToColor(jsonElement.get("strokeColor").getAsString());
            float strokeWidth = jsonElement.get("strokeWidth").getAsFloat();
            return new CircleElement(x, y, width, height, fillColor, strokeColor, strokeWidth);
        } else if (type.equals("path")) {
            Color fillColor = stringToColor(jsonElement.get("fillColor").getAsString());
            Color strokeColor = stringToColor(jsonElement.get("strokeColor").getAsString());
            float strokeWidth = jsonElement.get("strokeWidth").getAsFloat();
            String pathData = jsonElement.get("pathData").getAsString();
            java.awt.geom.Path2D.Double path = stringToPath(pathData);
            return new PathElement(path, x, y, width, height, fillColor, strokeColor, strokeWidth);
        } else if (type.equals("text") || type.equals("svg-text")) {
            String text = jsonElement.get("text").getAsString();
            String fontName = jsonElement.get("fontName").getAsString();
            int fontSize = jsonElement.get("fontSize").getAsInt();
            int fontStyle = jsonElement.get("fontStyle").getAsInt();
            Font font = new Font(fontName, fontStyle, fontSize);
            
            if (type.equals("svg-text")) {
                Color textColor = stringToColor(jsonElement.get("textColor").getAsString());
                double rotation = jsonElement.has("rotation") ? jsonElement.get("rotation").getAsDouble() : 0.0;
                return new SVGTextElement(x, y, width, height, text, font, textColor, rotation);
            } else if (jsonElement.has("textColor")) {
                Color textColor = stringToColor(jsonElement.get("textColor").getAsString());
                return new TextElementWithColor(x, y, width, height, text, font, textColor);
            } else {
                return new TextElement(x, y, width, height, text, font);
            }
        }
        
        return null;
    }
    
    private String colorToString(Color color) {
        if (color == null) return "none";
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private Color stringToColor(String colorStr) {
        if (colorStr == null || colorStr.equals("none")) return null;
        try {
            return Color.decode(colorStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String pathToString(java.awt.geom.Path2D.Double path) {
        StringBuilder sb = new StringBuilder();
        java.awt.geom.PathIterator pi = path.getPathIterator(null);
        double[] coords = new double[6];
        
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case java.awt.geom.PathIterator.SEG_MOVETO:
                    sb.append("M").append(coords[0]).append(",").append(coords[1]).append(" ");
                    break;
                case java.awt.geom.PathIterator.SEG_LINETO:
                    sb.append("L").append(coords[0]).append(",").append(coords[1]).append(" ");
                    break;
                case java.awt.geom.PathIterator.SEG_QUADTO:
                    sb.append("Q").append(coords[0]).append(",").append(coords[1]).append(" ")
                      .append(coords[2]).append(",").append(coords[3]).append(" ");
                    break;
                case java.awt.geom.PathIterator.SEG_CUBICTO:
                    sb.append("C").append(coords[0]).append(",").append(coords[1]).append(" ")
                      .append(coords[2]).append(",").append(coords[3]).append(" ")
                      .append(coords[4]).append(",").append(coords[5]).append(" ");
                    break;
                case java.awt.geom.PathIterator.SEG_CLOSE:
                    sb.append("Z ");
                    break;
            }
            pi.next();
        }
        
        return sb.toString().trim();
    }
    
    private java.awt.geom.Path2D.Double stringToPath(String pathData) {
        java.awt.geom.Path2D.Double path = new java.awt.geom.Path2D.Double();
        
        String[] commands = pathData.split(" ");
        for (String command : commands) {
            if (command.isEmpty()) continue;
            
            // Validate command has at least one character
            if (command.length() == 0) continue;
            
            char cmd = command.charAt(0);
            String coordsStr = command.substring(1);
            if (coordsStr.isEmpty() && cmd != 'Z') continue;
            
            String[] coords = coordsStr.split(",");
            
            try {
                switch (cmd) {
                    case 'M':
                        path.moveTo(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
                        break;
                    case 'L':
                        path.lineTo(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
                        break;
                    case 'Q':
                        path.quadTo(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]),
                                   Double.parseDouble(coords[2]), Double.parseDouble(coords[3]));
                        break;
                    case 'C':
                        path.curveTo(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]),
                                    Double.parseDouble(coords[2]), Double.parseDouble(coords[3]),
                                    Double.parseDouble(coords[4]), Double.parseDouble(coords[5]));
                        break;
                    case 'Z':
                        path.closePath();
                        break;
                }
            } catch (Exception e) {
                System.err.println("Error parsing path command '" + command + "' in path data: " + 
                    pathData.substring(0, Math.min(100, pathData.length())));
            }
        }
        
        return path;
    }
}
