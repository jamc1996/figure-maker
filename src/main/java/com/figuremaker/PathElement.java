package com.figuremaker;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public class PathElement extends CanvasElement {
    private Path2D.Double path;
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    
    public PathElement(Path2D.Double path, int x, int y, int width, int height, 
                       Color fillColor, Color strokeColor, float strokeWidth) {
        super(x, y, width, height);
        this.path = path;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Save original transform
        var oldTransform = g2d.getTransform();
        
        // Translate to element position
        g2d.translate(x, y);
        
        // Draw filled path
        if (fillColor != null) {
            g2d.setColor(fillColor);
            g2d.fill(path);
        }
        
        // Draw stroke
        if (strokeColor != null && strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.draw(path);
        }
        
        // Restore transform
        g2d.setTransform(oldTransform);
        
        // Draw selection border
        if (selected) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2d.drawRect(x - 2, y - 2, width + 4, height + 4);
        }
    }
    
    @Override
    public String getType() {
        return "path";
    }
    
    public Path2D.Double getPath() {
        return path;
    }
    
    public Color getFillColor() {
        return fillColor;
    }
    
    public Color getStrokeColor() {
        return strokeColor;
    }
    
    public float getStrokeWidth() {
        return strokeWidth;
    }
}
