package com.figuremaker;

import java.awt.*;

public class TextElementWithColor extends TextElement {
    private Color textColor;
    
    public TextElementWithColor(int x, int y, int width, int height, String text, Font font, Color textColor) {
        super(x, y, width, height, text, font);
        this.textColor = textColor;
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
        
        if (!isEditing()) {
            // Draw text with the specified color
            g2d.setColor(textColor != null ? textColor : Color.BLACK);
            g2d.setFont(getFont());
            
            FontMetrics fm = g2d.getFontMetrics();
            String[] lines = getText().split("\n");
            int lineHeight = fm.getHeight();
            int textY = y + fm.getAscent() + 5;
            
            for (String line : lines) {
                g2d.drawString(line, x + 5, textY);
                textY += lineHeight;
            }
        }
        
        if (selected && !isEditing()) {
            // Draw resize handles
            int handleSize = 6;
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - handleSize/2, y - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + width - handleSize/2, y - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + width - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
        }
    }
    
    public Color getTextColor() {
        return textColor;
    }
    
    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }
}
