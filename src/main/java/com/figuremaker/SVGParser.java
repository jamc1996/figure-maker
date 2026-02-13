package com.figuremaker;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SVGParser {
    
    public static List<CanvasElement> parseSVG(File svgFile) throws Exception {
        List<CanvasElement> elements = new ArrayList<>();
        
        // Create a DOM document from the SVG file
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        Document doc = factory.createDocument(svgFile.toURI().toString());
        
        // Get the root SVG element
        Element svgRoot = doc.getDocumentElement();
        
        // Parse all child elements
        parseElement(svgRoot, elements, 0, 0);
        
        return elements;
    }
    
    private static void parseElement(Element element, List<CanvasElement> elements, int offsetX, int offsetY) {
        String tagName = element.getTagName().toLowerCase();
        
        // Parse transform attribute if present
        String transform = element.getAttribute("transform");
        int[] translation = parseTransform(transform);
        int currentOffsetX = offsetX + translation[0];
        int currentOffsetY = offsetY + translation[1];
        
        switch (tagName) {
            case "rect":
                parseRect(element, elements, currentOffsetX, currentOffsetY);
                break;
            case "circle":
                parseCircle(element, elements, currentOffsetX, currentOffsetY);
                break;
            case "ellipse":
                parseEllipse(element, elements, currentOffsetX, currentOffsetY);
                break;
            case "path":
                parsePath(element, elements, currentOffsetX, currentOffsetY);
                break;
            case "g":
            case "svg":
                // Recursively parse group elements
                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        parseElement((Element) child, elements, currentOffsetX, currentOffsetY);
                    }
                }
                break;
        }
    }
    
    private static void parseRect(Element rectElement, List<CanvasElement> elements, int offsetX, int offsetY) {
        try {
            double x = parseLength(rectElement.getAttribute("x"));
            double y = parseLength(rectElement.getAttribute("y"));
            double width = parseLength(rectElement.getAttribute("width"));
            double height = parseLength(rectElement.getAttribute("height"));
            
            Color fillColor = parseColor(rectElement.getAttribute("fill"));
            Color strokeColor = parseColor(rectElement.getAttribute("stroke"));
            float strokeWidth = parseStrokeWidth(rectElement.getAttribute("stroke-width"));
            
            // Apply style attribute if present
            String style = rectElement.getAttribute("style");
            if (style != null && !style.isEmpty()) {
                Color[] colors = parseStyle(style);
                if (colors[0] != null) fillColor = colors[0];
                if (colors[1] != null) strokeColor = colors[1];
            }
            
            RectElement rect = new RectElement(
                (int) x + offsetX, 
                (int) y + offsetY, 
                (int) width, 
                (int) height, 
                fillColor, 
                strokeColor, 
                strokeWidth
            );
            elements.add(rect);
        } catch (Exception e) {
            System.err.println("Error parsing rect element (x=" + rectElement.getAttribute("x") + 
                ", y=" + rectElement.getAttribute("y") + "): " + e.getMessage());
        }
    }
    
    private static void parseCircle(Element circleElement, List<CanvasElement> elements, int offsetX, int offsetY) {
        try {
            double cx = parseLength(circleElement.getAttribute("cx"));
            double cy = parseLength(circleElement.getAttribute("cy"));
            double r = parseLength(circleElement.getAttribute("r"));
            
            Color fillColor = parseColor(circleElement.getAttribute("fill"));
            Color strokeColor = parseColor(circleElement.getAttribute("stroke"));
            float strokeWidth = parseStrokeWidth(circleElement.getAttribute("stroke-width"));
            
            // Apply style attribute if present
            String style = circleElement.getAttribute("style");
            if (style != null && !style.isEmpty()) {
                Color[] colors = parseStyle(style);
                if (colors[0] != null) fillColor = colors[0];
                if (colors[1] != null) strokeColor = colors[1];
            }
            
            CircleElement circle = new CircleElement(
                (int) (cx - r) + offsetX,
                (int) (cy - r) + offsetY,
                (int) (r * 2),
                (int) (r * 2),
                fillColor,
                strokeColor,
                strokeWidth
            );
            elements.add(circle);
        } catch (Exception e) {
            System.err.println("Error parsing circle element (cx=" + circleElement.getAttribute("cx") + 
                ", cy=" + circleElement.getAttribute("cy") + "): " + e.getMessage());
        }
    }
    
    private static void parseEllipse(Element ellipseElement, List<CanvasElement> elements, int offsetX, int offsetY) {
        try {
            double cx = parseLength(ellipseElement.getAttribute("cx"));
            double cy = parseLength(ellipseElement.getAttribute("cy"));
            double rx = parseLength(ellipseElement.getAttribute("rx"));
            double ry = parseLength(ellipseElement.getAttribute("ry"));
            
            Color fillColor = parseColor(ellipseElement.getAttribute("fill"));
            Color strokeColor = parseColor(ellipseElement.getAttribute("stroke"));
            float strokeWidth = parseStrokeWidth(ellipseElement.getAttribute("stroke-width"));
            
            // Apply style attribute if present
            String style = ellipseElement.getAttribute("style");
            if (style != null && !style.isEmpty()) {
                Color[] colors = parseStyle(style);
                if (colors[0] != null) fillColor = colors[0];
                if (colors[1] != null) strokeColor = colors[1];
            }
            
            // Note: Using CircleElement to represent ellipse since both are ovals
            CircleElement ellipseCircle = new CircleElement(
                (int) (cx - rx) + offsetX,
                (int) (cy - ry) + offsetY,
                (int) (rx * 2),
                (int) (ry * 2),
                fillColor,
                strokeColor,
                strokeWidth
            );
            elements.add(ellipseCircle);
        } catch (Exception e) {
            System.err.println("Error parsing ellipse element (cx=" + ellipseElement.getAttribute("cx") + 
                ", cy=" + ellipseElement.getAttribute("cy") + "): " + e.getMessage());
        }
    }
    
    private static void parsePath(Element pathElement, List<CanvasElement> elements, int offsetX, int offsetY) {
        try {
            String d = pathElement.getAttribute("d");
            if (d == null || d.isEmpty()) return;
            
            // Check for unsupported commands (A=arc, S=smooth curve, T=smooth quadratic)
            // SVG commands can appear anywhere in the path, not necessarily with word boundaries
            String unsupportedCmd = "";
            if (d.toUpperCase().contains("A") && d.matches(".*[,\\s\\d][Aa][,\\s\\d].*")) {
                unsupportedCmd = "Arc (A)";
            } else if (d.toUpperCase().contains("S") && d.matches(".*[,\\s\\d][Ss][,\\s\\d].*")) {
                unsupportedCmd = "Smooth curve (S)";
            } else if (d.toUpperCase().contains("T") && d.matches(".*[,\\s\\d][Tt][,\\s\\d].*")) {
                unsupportedCmd = "Smooth quadratic (T)";
            }
            
            if (!unsupportedCmd.isEmpty()) {
                System.err.println("Warning: Skipping path with unsupported command: " + unsupportedCmd + ". " +
                    "Path data: " + d.substring(0, Math.min(80, d.length())) + 
                    (d.length() > 80 ? "..." : ""));
                return;
            }
            
            Color fillColor = parseColor(pathElement.getAttribute("fill"));
            Color strokeColor = parseColor(pathElement.getAttribute("stroke"));
            float strokeWidth = parseStrokeWidth(pathElement.getAttribute("stroke-width"));
            
            // Apply style attribute if present
            String style = pathElement.getAttribute("style");
            if (style != null && !style.isEmpty()) {
                Color[] colors = parseStyle(style);
                if (colors[0] != null) fillColor = colors[0];
                if (colors[1] != null) strokeColor = colors[1];
            }
            
            Path2D.Double path = parseSVGPath(d);
            
            // Calculate bounding box
            Rectangle bounds = path.getBounds();
            
            PathElement pathElem = new PathElement(
                path,
                bounds.x + offsetX,
                bounds.y + offsetY,
                bounds.width,
                bounds.height,
                fillColor,
                strokeColor,
                strokeWidth
            );
            elements.add(pathElem);
        } catch (Exception e) {
            System.err.println("Error parsing path element (d=" + 
                pathElement.getAttribute("d").substring(0, Math.min(50, pathElement.getAttribute("d").length())) + 
                "...): " + e.getMessage());
        }
    }
    
    private static Path2D.Double parseSVGPath(String d) {
        Path2D.Double path = new Path2D.Double();
        
        // Simple path parser - handles M, L, H, V, C, Q, Z commands
        // Note: This parser supports basic SVG path commands but does not handle
        // advanced commands like A (arc), S (smooth curve continuation), or T (smooth quadratic)
        Pattern pattern = Pattern.compile("([MLHVCQZmlhvcqz])([^MLHVCQZmlhvcqz]*)");
        Matcher matcher = pattern.matcher(d);
        
        double currentX = 0, currentY = 0;
        double startX = 0, startY = 0;
        
        while (matcher.find()) {
            String command = matcher.group(1);
            String coords = matcher.group(2).trim();
            
            if (coords.isEmpty() && !command.equalsIgnoreCase("Z")) continue;
            
            String[] values = coords.split("[,\\s]+");
            
            switch (command) {
                case "M": // Move to (absolute)
                    currentX = Double.parseDouble(values[0]);
                    currentY = Double.parseDouble(values[1]);
                    startX = currentX;
                    startY = currentY;
                    path.moveTo(currentX, currentY);
                    break;
                case "m": // Move to (relative)
                    currentX += Double.parseDouble(values[0]);
                    currentY += Double.parseDouble(values[1]);
                    startX = currentX;
                    startY = currentY;
                    path.moveTo(currentX, currentY);
                    break;
                case "L": // Line to (absolute)
                    for (int i = 0; i < values.length; i += 2) {
                        currentX = Double.parseDouble(values[i]);
                        currentY = Double.parseDouble(values[i + 1]);
                        path.lineTo(currentX, currentY);
                    }
                    break;
                case "l": // Line to (relative)
                    for (int i = 0; i < values.length; i += 2) {
                        currentX += Double.parseDouble(values[i]);
                        currentY += Double.parseDouble(values[i + 1]);
                        path.lineTo(currentX, currentY);
                    }
                    break;
                case "H": // Horizontal line (absolute)
                    currentX = Double.parseDouble(values[0]);
                    path.lineTo(currentX, currentY);
                    break;
                case "h": // Horizontal line (relative)
                    currentX += Double.parseDouble(values[0]);
                    path.lineTo(currentX, currentY);
                    break;
                case "V": // Vertical line (absolute)
                    currentY = Double.parseDouble(values[0]);
                    path.lineTo(currentX, currentY);
                    break;
                case "v": // Vertical line (relative)
                    currentY += Double.parseDouble(values[0]);
                    path.lineTo(currentX, currentY);
                    break;
                case "Z": // Close path
                case "z":
                    path.closePath();
                    currentX = startX;
                    currentY = startY;
                    break;
            }
        }
        
        return path;
    }
    
    private static double parseLength(String value) {
        if (value == null || value.isEmpty()) return 0;
        // Remove units like px, pt, etc.
        value = value.replaceAll("[a-zA-Z%]", "").trim();
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private static Color parseColor(String color) {
        if (color == null || color.isEmpty() || color.equals("none")) return null;
        
        color = color.trim();
        
        // Handle hex colors
        if (color.startsWith("#")) {
            try {
                if (color.length() == 7) {
                    return Color.decode(color);
                } else if (color.length() == 4) {
                    // Short hex format #RGB -> #RRGGBB
                    String r = String.valueOf(color.charAt(1)) + color.charAt(1);
                    String g = String.valueOf(color.charAt(2)) + color.charAt(2);
                    String b = String.valueOf(color.charAt(3)) + color.charAt(3);
                    return Color.decode("#" + r + g + b);
                }
            } catch (NumberFormatException e) {
                return Color.BLACK;
            }
        }
        
        // Handle rgb() format
        if (color.startsWith("rgb(")) {
            color = color.substring(4, color.length() - 1);
            String[] parts = color.split(",");
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return new Color(r, g, b);
            } catch (Exception e) {
                return Color.BLACK;
            }
        }
        
        // Handle named colors
        switch (color.toLowerCase()) {
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            case "blue": return Color.BLUE;
            case "yellow": return Color.YELLOW;
            case "cyan": return Color.CYAN;
            case "magenta": return Color.MAGENTA;
            case "gray": case "grey": return Color.GRAY;
            case "orange": return Color.ORANGE;
            case "pink": return Color.PINK;
            default: return Color.BLACK;
        }
    }
    
    private static float parseStrokeWidth(String width) {
        if (width == null || width.isEmpty()) return 1.0f;
        try {
            return Float.parseFloat(width.replaceAll("[a-zA-Z%]", "").trim());
        } catch (NumberFormatException e) {
            return 1.0f;
        }
    }
    
    private static Color[] parseStyle(String style) {
        Color[] colors = new Color[2]; // [fill, stroke]
        
        String[] properties = style.split(";");
        for (String prop : properties) {
            String[] keyValue = prop.split(":");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                if (key.equals("fill")) {
                    colors[0] = parseColor(value);
                } else if (key.equals("stroke")) {
                    colors[1] = parseColor(value);
                }
            }
        }
        
        return colors;
    }
    
    private static int[] parseTransform(String transform) {
        int[] translation = new int[2]; // [x, y]
        
        if (transform == null || transform.isEmpty()) return translation;
        
        // Simple parser for translate() transform
        // Note: Only handles translate() transforms. Other transforms like
        // rotate(), scale(), skew(), and matrix() are not currently supported.
        Pattern pattern = Pattern.compile("translate\\(([^,\\)]+)[,\\s]*([^\\)]+)\\)");
        Matcher matcher = pattern.matcher(transform);
        
        if (matcher.find()) {
            try {
                translation[0] = (int) Double.parseDouble(matcher.group(1).trim());
                translation[1] = (int) Double.parseDouble(matcher.group(2).trim());
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
        
        return translation;
    }
}
