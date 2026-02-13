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
    
    public FigureCanvas() {
        elements = new ArrayList<>();
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
        setLayout(null);
        
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
        
        // Add mouse motion listener for cursor changes
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getX(), e.getY());
            }
        });
    }
    
    private void updateCursor(int x, int y) {
        if (selectedElement instanceof ImageElement) {
            ImageElement imageElement = (ImageElement) selectedElement;
            int handle = imageElement.getResizeHandleAt(x, y);
            
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
            resizeHandle = imageElement.getResizeHandleAt(e.getX(), e.getY());
            
            if (resizeHandle >= 0) {
                isResizing = true;
                isDragging = false;
                dragStart = e.getPoint();
                elementDragStart = new Point(selectedElement.getX(), selectedElement.getY());
                startWidth = selectedElement.getWidth();
                startHeight = selectedElement.getHeight();
                return;
            }
        }
        
        // Find element at click position
        CanvasElement clickedElement = findElementAt(e.getX(), e.getY());
        
        if (clickedElement != null) {
            if (selectedElement != null) {
                selectedElement.setSelected(false);
            }
            selectedElement = clickedElement;
            selectedElement.setSelected(true);
            
            dragStart = e.getPoint();
            elementDragStart = new Point(selectedElement.getX(), selectedElement.getY());
            isDragging = false;
            isResizing = false;
        } else {
            if (selectedElement != null) {
                selectedElement.setSelected(false);
                selectedElement = null;
            }
        }
        
        // Show position dialog on right-click
        if (e.getButton() == MouseEvent.BUTTON3 && selectedElement != null) {
            showPositionDialog();
        }
        
        repaint();
    }
    
    private void handleMouseDragged(MouseEvent e) {
        if (selectedElement != null && dragStart != null) {
            if (isResizing) {
                int dx = e.getX() - dragStart.x;
                int dy = e.getY() - dragStart.y;
                
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
                            // Use the larger dimension change to preserve aspect ratio
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
                int dx = e.getX() - dragStart.x;
                int dy = e.getY() - dragStart.y;
                
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
        
        for (CanvasElement element : elements) {
            element.draw(g);
        }
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
            } else if (element instanceof TextElement) {
                TextElement textElement = (TextElement) element;
                jsonElement.addProperty("text", textElement.getText());
                jsonElement.addProperty("fontName", textElement.getFont().getName());
                jsonElement.addProperty("fontSize", textElement.getFont().getSize());
                jsonElement.addProperty("fontStyle", textElement.getFont().getStyle());
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
                }
            }
            
            repaint();
        }
    }
}
