package com.figuremaker;

import java.awt.*;

public abstract class CanvasElement {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean selected;
    
    public CanvasElement(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.selected = false;
    }
    
    public abstract void draw(Graphics g);
    
    public boolean contains(int px, int py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public abstract String getType();
}
