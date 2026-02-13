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
    
    // Pre-compiled patterns for detecting unsupported SVG path commands
    // Note: Arc commands are now supported
    private static final Pattern SMOOTH_CURVE_PATTERN = Pattern.compile("([,\\s\\d]|^)[Ss]([,\\s\\d].*|$)");
    private static final Pattern SMOOTH_QUAD_PATTERN = Pattern.compile("([,\\s\\d]|^)[Tt]([,\\s\\d].*|$)");
    
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
            case "text":
                parseText(element, elements, currentOffsetX, currentOffsetY);
                break;
            case "g":
                // Parse group element
                parseGroup(element, elements, currentOffsetX, currentOffsetY);
                break;
            case "svg":
                // Recursively parse SVG root children without creating a group
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
    
    private static void parseGroup(Element groupElement, List<CanvasElement> elements, int offsetX, int offsetY) {
        try {
            // Get group ID if present
            String id = groupElement.getAttribute("id");
            
            // Check if this is a clipping mask
            String clipPath = groupElement.getAttribute("clip-path");
            boolean isClippingMask = clipPath != null && !clipPath.isEmpty();
            
            // Parse all children into a temporary list
            List<CanvasElement> groupChildren = new ArrayList<>();
            NodeList children = groupElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element) {
                    parseElement((Element) child, groupChildren, offsetX, offsetY);
                }
            }
            
            // If group has children, create a GroupElement
            if (!groupChildren.isEmpty()) {
                // Calculate bounding box
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                
                for (CanvasElement child : groupChildren) {
                    minX = Math.min(minX, child.getX());
                    minY = Math.min(minY, child.getY());
                    maxX = Math.max(maxX, child.getX() + child.getWidth());
                    maxY = Math.max(maxY, child.getY() + child.getHeight());
                }
                
                GroupElement group = new GroupElement(minX, minY, maxX - minX, maxY - minY, id);
                group.setClippingMask(isClippingMask);
                
                for (CanvasElement child : groupChildren) {
                    group.addChild(child);
                }
                
                elements.add(group);
            }
        } catch (Exception e) {
            System.err.println("Error parsing group element: " + e.getMessage());
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
            
            // Check for unsupported commands using pre-compiled patterns
            // Note: Arc (A) commands are now supported
            String unsupportedCmd = "";
            if (SMOOTH_CURVE_PATTERN.matcher(d).find()) {
                unsupportedCmd = "Smooth curve (S)";
            } else if (SMOOTH_QUAD_PATTERN.matcher(d).find()) {
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
            
            // Translate the path so its bounding box starts at (0, 0)
            // This is necessary because PathElement.draw() translates to (x, y) before drawing
            Path2D.Double translatedPath = new Path2D.Double();
            translatedPath.append(path.getPathIterator(java.awt.geom.AffineTransform.getTranslateInstance(-bounds.x, -bounds.y)), false);
            
            PathElement pathElem = new PathElement(
                translatedPath,
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
    
    private static void parseText(Element textElement, List<CanvasElement> elements, int offsetX, int offsetY) {
        try {
            // Get text content
            String textContent = textElement.getTextContent();
            if (textContent == null || textContent.trim().isEmpty()) return;
            
            // Get position
            double x = parseLength(textElement.getAttribute("x"));
            double y = parseLength(textElement.getAttribute("y"));
            
            // Parse font properties
            String fontFamily = textElement.getAttribute("font-family");
            if (fontFamily.isEmpty()) fontFamily = "Arial";
            
            String fontSizeStr = textElement.getAttribute("font-size");
            int fontSize = 14; // default
            if (!fontSizeStr.isEmpty()) {
                fontSize = (int) parseLength(fontSizeStr);
            }
            
            String fontWeight = textElement.getAttribute("font-weight");
            boolean isBold = fontWeight.equals("bold") || fontWeight.equals("700");
            
            String fontStyle = textElement.getAttribute("font-style");
            boolean isItalic = fontStyle.equals("italic");
            
            int fontStyleCode = Font.PLAIN;
            if (isBold && isItalic) fontStyleCode = Font.BOLD | Font.ITALIC;
            else if (isBold) fontStyleCode = Font.BOLD;
            else if (isItalic) fontStyleCode = Font.ITALIC;
            
            Font font = new Font(fontFamily, fontStyleCode, fontSize);
            
            // Parse fill color for text
            Color fillColor = parseColor(textElement.getAttribute("fill"));
            if (fillColor == null) fillColor = Color.BLACK; // SVG default for text
            
            // Apply style attribute if present
            String style = textElement.getAttribute("style");
            if (style != null && !style.isEmpty()) {
                Color[] colors = parseStyle(style);
                if (colors[0] != null) fillColor = colors[0];
            }
            
            // Estimate text dimensions (rough approximation)
            int estimatedWidth = textContent.length() * fontSize / 2;
            int estimatedHeight = fontSize + 10;
            
            // Create text element with custom TextElement that supports color
            TextElementWithColor textElem = new TextElementWithColor(
                (int)x + offsetX,
                (int)y + offsetY - fontSize, // SVG y is baseline, adjust for top
                estimatedWidth,
                estimatedHeight,
                textContent.trim(),
                font,
                fillColor
            );
            elements.add(textElem);
        } catch (Exception e) {
            System.err.println("Error parsing text element: " + e.getMessage());
        }
    }
    
    private static Path2D.Double parseSVGPath(String d) {
        Path2D.Double path = new Path2D.Double();
        
        // Path parser - handles M, L, H, V, C, Q, A, Z commands
        // Note: This parser now supports arc (A) commands
        Pattern pattern = Pattern.compile("([MLHVCQAZmlhvcqaz])([^MLHVCQAZmlhvcqaz]*)");
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
                case "A": // Arc (absolute)
                case "a": // Arc (relative)
                    // Arc parameters: rx ry x-axis-rotation large-arc-flag sweep-flag x y
                    for (int i = 0; i + 7 <= values.length; i += 7) {
                        double rx = Double.parseDouble(values[i]);
                        double ry = Double.parseDouble(values[i + 1]);
                        double xAxisRotation = Double.parseDouble(values[i + 2]);
                        boolean largeArcFlag = Double.parseDouble(values[i + 3]) != 0;
                        boolean sweepFlag = Double.parseDouble(values[i + 4]) != 0;
                        double x = Double.parseDouble(values[i + 5]);
                        double y = Double.parseDouble(values[i + 6]);
                        
                        if (command.equals("a")) {
                            // Relative arc
                            x += currentX;
                            y += currentY;
                        }
                        
                        // Convert arc to cubic Bezier curves
                        arcToBezier(path, currentX, currentY, x, y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag);
                        
                        currentX = x;
                        currentY = y;
                    }
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
    
    /**
     * Convert an SVG arc to cubic Bezier curves.
     * Based on the SVG specification: https://www.w3.org/TR/SVG/implnotes.html#ArcImplementationNotes
     */
    private static void arcToBezier(Path2D.Double path, double x1, double y1, double x2, double y2,
                                    double rx, double ry, double angle, boolean largeArcFlag, boolean sweepFlag) {
        // Handle degenerate cases
        if (x1 == x2 && y1 == y2) {
            return; // Start and end points are the same
        }
        
        if (rx == 0 || ry == 0) {
            path.lineTo(x2, y2); // Radii are zero, draw a line
            return;
        }
        
        // Ensure radii are positive
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        
        // Convert angle from degrees to radians
        double angleRad = Math.toRadians(angle);
        double cosAngle = Math.cos(angleRad);
        double sinAngle = Math.sin(angleRad);
        
        // Step 1: Compute (x1', y1')
        double dx = (x1 - x2) / 2.0;
        double dy = (y1 - y2) / 2.0;
        double x1Prime = cosAngle * dx + sinAngle * dy;
        double y1Prime = -sinAngle * dx + cosAngle * dy;
        
        // Step 2: Correct radii if needed
        double lambda = (x1Prime * x1Prime) / (rx * rx) + (y1Prime * y1Prime) / (ry * ry);
        if (lambda > 1) {
            rx *= Math.sqrt(lambda);
            ry *= Math.sqrt(lambda);
        }
        
        // Step 3: Compute center point (cx', cy')
        double sign = (largeArcFlag != sweepFlag) ? 1 : -1;
        double sq = Math.max(0, (rx * rx * ry * ry - rx * rx * y1Prime * y1Prime - ry * ry * x1Prime * x1Prime) 
                             / (rx * rx * y1Prime * y1Prime + ry * ry * x1Prime * x1Prime));
        double coef = sign * Math.sqrt(sq);
        double cxPrime = coef * rx * y1Prime / ry;
        double cyPrime = -coef * ry * x1Prime / rx;
        
        // Step 4: Compute center point (cx, cy)
        double cx = cosAngle * cxPrime - sinAngle * cyPrime + (x1 + x2) / 2.0;
        double cy = sinAngle * cxPrime + cosAngle * cyPrime + (y1 + y2) / 2.0;
        
        // Step 5: Compute angles
        double theta1 = Math.atan2((y1Prime - cyPrime) / ry, (x1Prime - cxPrime) / rx);
        double dTheta = Math.atan2((-y1Prime - cyPrime) / ry, (-x1Prime - cxPrime) / rx) - theta1;
        
        // Adjust dTheta based on sweep flag
        if (sweepFlag && dTheta < 0) {
            dTheta += 2 * Math.PI;
        } else if (!sweepFlag && dTheta > 0) {
            dTheta -= 2 * Math.PI;
        }
        
        // Handle degenerate case where arc spans 0 radians
        if (Math.abs(dTheta) < 1e-10) {
            return;
        }
        
        // Convert arc to cubic Bezier curves
        int segments = Math.max(1, (int) Math.ceil(Math.abs(dTheta) / (Math.PI / 2.0)));
        double delta = dTheta / segments;
        
        // Calculate tangent factor, with guard against division by zero
        double sinHalfDelta = Math.sin(delta / 2.0);
        if (Math.abs(sinHalfDelta) < 1e-10) {
            return; // Degenerate arc
        }
        double t = (8.0 / 3.0) * Math.sin(delta / 4.0) * Math.sin(delta / 4.0) / sinHalfDelta;
        
        for (int i = 0; i < segments; i++) {
            double theta = theta1 + i * delta;
            double thetaNext = theta + delta;
            
            double cos1 = Math.cos(theta);
            double sin1 = Math.sin(theta);
            double cos2 = Math.cos(thetaNext);
            double sin2 = Math.sin(thetaNext);
            
            // First control point
            double cp1x = cos1 - sin1 * t;
            double cp1y = sin1 + cos1 * t;
            
            // Second control point
            double cp2x = cos2 + sin2 * t;
            double cp2y = sin2 - cos2 * t;
            
            // Transform back to original coordinate system
            double cp1xTransformed = rx * cp1x;
            double cp1yTransformed = ry * cp1y;
            double cp2xTransformed = rx * cp2x;
            double cp2yTransformed = ry * cp2y;
            double endXTransformed = rx * cos2;
            double endYTransformed = ry * sin2;
            
            // Apply rotation and translation
            double cp1xFinal = cosAngle * cp1xTransformed - sinAngle * cp1yTransformed + cx;
            double cp1yFinal = sinAngle * cp1xTransformed + cosAngle * cp1yTransformed + cy;
            double cp2xFinal = cosAngle * cp2xTransformed - sinAngle * cp2yTransformed + cx;
            double cp2yFinal = sinAngle * cp2xTransformed + cosAngle * cp2yTransformed + cy;
            double endXFinal = cosAngle * endXTransformed - sinAngle * endYTransformed + cx;
            double endYFinal = sinAngle * endXTransformed + cosAngle * endYTransformed + cy;
            
            path.curveTo(cp1xFinal, cp1yFinal, cp2xFinal, cp2yFinal, endXFinal, endYFinal);
        }
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
