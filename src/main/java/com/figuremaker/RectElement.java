package com.figuremaker;

import java.awt.*;

public class RectElement extends CanvasElement {
    private Color fillColor;
    private Color strokeColor;
    private float strokeWidth;
    
    public RectElement(int x, int y, int width, int height, Color fillColor, Color strokeColor, float strokeWidth) {
        super(x, y, width, height);
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw filled rectangle
        if (fillColor != null) {
            g2d.setColor(fillColor);
            g2d.fillRect(x, y, width, height);
        }
        
        // Draw stroke
        if (strokeColor != null && strokeWidth > 0) {
            g2d.setColor(strokeColor);
            g2d.setStroke(new BasicStroke(strokeWidth));
            g2d.drawRect(x, y, width, height);
        }
        
        // Draw selection border
        if (selected) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2d.drawRect(x - 2, y - 2, width + 4, height + 4);
        }
    }
    
    @Override
    public String getType() {
        return "rect";
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
