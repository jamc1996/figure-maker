package com.figuremaker;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * A text element specifically for SVG-imported text.
 * Unlike TextElement, this renders text at the exact SVG coordinates
 * without boxes, backgrounds, or padding.
 */
public class SVGTextElement extends CanvasElement {
    private String text;
    private Font font;
    private Color textColor;
    private double rotation; // in degrees
    
    public SVGTextElement(int x, int y, int width, int height, String text, Font font, Color textColor, double rotation) {
        super(x, y, width, height);
        this.text = text;
        this.font = font;
        this.textColor = textColor != null ? textColor : Color.BLACK;
        this.rotation = rotation;
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Save original transform
        AffineTransform originalTransform = g2d.getTransform();
        
        // Apply rotation if present
        if (rotation != 0) {
            g2d.rotate(Math.toRadians(rotation), x, y);
        }
        
        // Draw text with the specified color at exact SVG coordinates
        g2d.setColor(textColor);
        g2d.setFont(font);
        
        // In SVG, y coordinate is at the baseline
        // We position the element's y at the baseline, so draw text there
        g2d.drawString(text, x, y);
        
        // Restore original transform
        g2d.setTransform(originalTransform);
        
        // Draw selection indicator if selected
        if (selected) {
            FontMetrics fm = g2d.getFontMetrics(font);
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            
            // Draw dashed box around text bounds
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            int textY = y - fm.getAscent(); // Top of text
            
            if (rotation != 0) {
                g2d.rotate(Math.toRadians(rotation), x, y);
            }
            
            g2d.drawRect(x - 2, textY - 2, textWidth + 4, textHeight + 4);
            
            g2d.setTransform(originalTransform);
            
            // Draw resize handles
            int handleSize = 6;
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - handleSize/2, textY - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + textWidth - handleSize/2, textY - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x - handleSize/2, textY + textHeight - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + textWidth - handleSize/2, textY + textHeight - handleSize/2, handleSize, handleSize);
        }
    }
    
    @Override
    public boolean contains(int px, int py) {
        // For rotated text, we need to check if point is within the rotated bounds
        if (rotation != 0) {
            // Transform the point to the text's local coordinate system
            double cos = Math.cos(-Math.toRadians(rotation));
            double sin = Math.sin(-Math.toRadians(rotation));
            double dx = px - x;
            double dy = py - y;
            double localX = x + dx * cos - dy * sin;
            double localY = y + dx * sin + dy * cos;
            
            // Create a temporary graphics to get font metrics
            // This is a workaround since we don't have direct access to font metrics here
            // In practice, we'll use the stored width/height
            return localX >= x && localX <= x + width && localY >= y - height && localY <= y;
        }
        
        // For non-rotated text, use simple bounds check
        // Text baseline is at y, so text extends from y-height to y
        return px >= x && px <= x + width && py >= y - height && py <= y;
    }
    
    @Override
    public String getType() {
        return "svg-text";
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
    
    public Color getTextColor() {
        return textColor;
    }
    
    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }
    
    public double getRotation() {
        return rotation;
    }
    
    public void setRotation(double rotation) {
        this.rotation = rotation;
    }
}
