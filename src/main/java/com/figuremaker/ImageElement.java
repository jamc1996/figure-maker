package com.figuremaker;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ImageElement extends CanvasElement {
    private BufferedImage image;
    private String imagePath;
    
    public ImageElement(File imageFile, int x, int y) throws IOException {
        super(x, y, 0, 0);
        this.image = ImageIO.read(imageFile);
        this.imagePath = imageFile.getAbsolutePath();
        this.width = image.getWidth();
        this.height = image.getHeight();
    }
    
    public ImageElement(BufferedImage image, int x, int y, int width, int height, String imagePath) {
        super(x, y, width, height);
        this.image = image;
        this.imagePath = imagePath;
    }
    
    @Override
    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, x, y, width, height, null);
            
            if (selected) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.BLUE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(x, y, width, height);
                
                // Draw resize handles
                int handleSize = 6;
                g2d.fillRect(x - handleSize/2, y - handleSize/2, handleSize, handleSize);
                g2d.fillRect(x + width - handleSize/2, y - handleSize/2, handleSize, handleSize);
                g2d.fillRect(x - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
                g2d.fillRect(x + width - handleSize/2, y + height - handleSize/2, handleSize, handleSize);
            }
        }
    }
    
    @Override
    public String getType() {
        return "image";
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public BufferedImage getImage() {
        return image;
    }
    
    public String getImageAsBase64() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
    
    public static BufferedImage decodeBase64Image(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
    
    // Resize handle positions: 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
    public int getResizeHandleAt(int px, int py) {
        if (!selected) return -1;
        
        int handleSize = 6;
        int tolerance = 3;
        
        // Top-left
        if (Math.abs(px - x) <= handleSize/2 + tolerance && 
            Math.abs(py - y) <= handleSize/2 + tolerance) {
            return 0;
        }
        // Top-right
        if (Math.abs(px - (x + width)) <= handleSize/2 + tolerance && 
            Math.abs(py - y) <= handleSize/2 + tolerance) {
            return 1;
        }
        // Bottom-left
        if (Math.abs(px - x) <= handleSize/2 + tolerance && 
            Math.abs(py - (y + height)) <= handleSize/2 + tolerance) {
            return 2;
        }
        // Bottom-right
        if (Math.abs(px - (x + width)) <= handleSize/2 + tolerance && 
            Math.abs(py - (y + height)) <= handleSize/2 + tolerance) {
            return 3;
        }
        
        return -1;
    }
}
