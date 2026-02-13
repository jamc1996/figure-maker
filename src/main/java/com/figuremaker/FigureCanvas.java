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
            isDragging = true;
            int dx = e.getX() - dragStart.x;
            int dy = e.getY() - dragStart.y;
            
            selectedElement.setPosition(elementDragStart.x + dx, elementDragStart.y + dy);
            repaint();
        }
    }
    
    private void handleMouseReleased(MouseEvent e) {
        isDragging = false;
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
                        var image = ImageElement.decodeBase64Image(imageData);
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
