package com.figuremaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TextElement extends CanvasElement {
    private String text;
    private Font font;
    private JTextArea textArea;
    private boolean editing;
    
    public TextElement(int x, int y) {
        super(x, y, 200, 100);
        this.text = "Enter text here";
        this.font = new Font("Arial", Font.PLAIN, 14);
        this.editing = false;
    }
    
    public TextElement(int x, int y, int width, int height, String text, Font font) {
        super(x, y, width, height);
        this.text = text;
        this.font = font;
        this.editing = false;
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, width, height);
        
        // Draw border
        g2d.setColor(selected ? Color.BLUE : Color.GRAY);
        g2d.setStroke(new BasicStroke(selected ? 2 : 1));
        g2d.drawRect(x, y, width, height);
        
        if (!editing) {
            // Draw text
            g2d.setColor(Color.BLACK);
            g2d.setFont(font);
            
            FontMetrics fm = g2d.getFontMetrics();
            String[] lines = text.split("\n");
            int lineHeight = fm.getHeight();
            int textY = y + fm.getAscent() + 5;
            
            for (String line : lines) {
                g2d.drawString(line, x + 5, textY);
                textY += lineHeight;
            }
        }
        
        if (selected && !editing) {
            // Draw resize handles
            int handleSize = 6;
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - handleSize/2, y - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + width - handleSize/2, y - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + width - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
        }
    }
    
    @Override
    public String getType() {
        return "text";
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public Font getFont() {
        return font;
    }
    
    public void setFont(Font font) {
        this.font = font;
    }
    
    public void startEditing(JPanel canvas) {
        editing = true;
        textArea = new JTextArea(text);
        textArea.setFont(font);
        textArea.setBounds(x, y, width, height);
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                stopEditing(canvas);
            }
        });
        
        canvas.add(textArea);
        textArea.requestFocus();
        canvas.revalidate();
        canvas.repaint();
    }
    
    public void stopEditing(JPanel canvas) {
        if (editing && textArea != null) {
            text = textArea.getText();
            canvas.remove(textArea);
            textArea = null;
            editing = false;
            canvas.revalidate();
            canvas.repaint();
        }
    }
    
    public boolean isEditing() {
        return editing;
    }
}
