package com.figuremaker;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GroupElement extends CanvasElement {
    private List<CanvasElement> children;
    private String groupId; // For tracking SVG groups
    private boolean isClippingMask;
    
    public GroupElement(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        this.isClippingMask = false;
    }
    
    public GroupElement(int x, int y, int width, int height, String groupId) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        this.groupId = groupId;
        this.isClippingMask = false;
    }
    
    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        
        // If this is a clipping mask, apply clipping to children
        Shape oldClip = null;
        if (isClippingMask) {
            oldClip = g2d.getClip();
            // Create a rectangular clip region
            Rectangle clipRect = new Rectangle(x, y, width, height);
            g2d.setClip(clipRect);
        }
        
        // Draw all children
        for (CanvasElement child : children) {
            child.draw(g);
        }
        
        // Restore original clip if we applied clipping
        if (isClippingMask) {
            g2d.setClip(oldClip);
        }
        
        // Draw selection border around the entire group
        if (selected) {
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
            g2d.drawRect(x - 2, y - 2, width + 4, height + 4);
            
            // Draw resize handles
            int handleSize = 6;
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x - handleSize/2, y - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + width - handleSize/2, y - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
            g2d.fillRect(x + width - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
        }
        
        // Visual indicator for clipping mask
        if (isClippingMask && selected) {
            g2d.setColor(new Color(255, 0, 0, 100)); // Semi-transparent red
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(x, y, width, height);
        }
    }
    
    @Override
    public boolean contains(int px, int py) {
        // Check if any child contains the point
        for (CanvasElement child : children) {
            if (child.contains(px, py)) {
                return true;
            }
        }
        return super.contains(px, py);
    }
    
    @Override
    public void setPosition(int newX, int newY) {
        // Calculate offset
        int dx = newX - x;
        int dy = newY - y;
        
        // Move all children by the same offset
        for (CanvasElement child : children) {
            child.setPosition(child.getX() + dx, child.getY() + dy);
        }
        
        // Update group position
        super.setPosition(newX, newY);
    }
    
    @Override
    public String getType() {
        return isClippingMask ? "clipping-mask" : "group";
    }
    
    public void addChild(CanvasElement element) {
        children.add(element);
        updateBounds();
    }
    
    public void removeChild(CanvasElement element) {
        children.remove(element);
        updateBounds();
    }
    
    public List<CanvasElement> getChildren() {
        return new ArrayList<>(children);
    }
    
    public void releaseClippingMask() {
        isClippingMask = false;
    }
    
    public boolean isClippingMask() {
        return isClippingMask;
    }
    
    public void setClippingMask(boolean isClippingMask) {
        this.isClippingMask = isClippingMask;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    private void updateBounds() {
        if (children.isEmpty()) {
            return;
        }
        
        // Calculate bounding box of all children
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        
        for (CanvasElement child : children) {
            minX = Math.min(minX, child.getX());
            minY = Math.min(minY, child.getY());
            maxX = Math.max(maxX, child.getX() + child.getWidth());
            maxY = Math.max(maxY, child.getY() + child.getHeight());
        }
        
        x = minX;
        y = minY;
        width = maxX - minX;
        height = maxY - minY;
    }
}
