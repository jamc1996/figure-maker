# Figure Maker

A Java desktop application for creating figures for academic publication.

## Features

- **Canvas Management**: Create new canvas or open previously saved canvases
  - Set custom canvas size when creating a new canvas
- **Image Support**: Load PNG and JPEG images from your filesystem
- **SVG Import**: Import SVG files with each element becoming individually editable
  - Supports rectangles, circles, ellipses, and paths (including arc commands)
  - Path commands supported: M, L, H, V, C, Q, A, Z (move, line, horizontal, vertical, cubic curve, quadratic curve, arc, close)
  - Preserves fill colors, stroke colors, and stroke widths
  - Handles grouped elements and basic transforms
- **Image Positioning**: 
  - Drag and drop images to position them on the canvas
  - Right-click on selected images to set exact position (distance from upper left corner)
- **Text Boxes**: Create and edit text boxes on the canvas
- **Selection Tools**:
  - Click and drag on empty canvas to select multiple objects in an area
  - Press Ctrl/Cmd+A to select all objects on the canvas
  - Delete multiple selected objects with Backspace/Delete
- **Zoom Controls**:
  - Ctrl/Cmd + scroll wheel to zoom in/out (Mac-style pinch zoom support)
  - Press `+` to zoom in
  - Press `-` to zoom out
  - Press `0` to reset zoom to 100%
- **Save/Load**: Save your work and load it later in `.fmk` format

## Requirements

- Java 11 or higher

## Building the Application

```bash
mvn clean package
```

This will create a self-contained JAR file (fat JAR) in the `target/` directory that includes all dependencies.

## Running the Application

```bash
java -jar target/figure-maker-1.0.0.jar
```

Or simply double-click the JAR file if your system is configured to run Java applications.

## Usage

### Creating a New Canvas
1. Launch the application
2. Click **File > New Canvas** to start fresh
3. Enter the desired canvas width and height in pixels
4. Click OK to create the canvas with the specified size

### Adding Images
1. Click **Edit > Add Image** or use the toolbar button
2. Select a PNG or JPEG file from your filesystem
3. The image will appear on the canvas at position (50, 50)

### Importing SVG Files
1. Click **File > Import SVG**
2. Select an SVG file from your filesystem
3. Each element in the SVG (rectangles, circles, ellipses, paths) will be imported as a separate, individually editable object on the canvas
4. You can then select, move, and manipulate each element independently

### Positioning Images
- **Drag and Drop**: Click and drag an image to move it
- **Manual Positioning**: Right-click on a selected image and enter exact X and Y coordinates

### Adding Text Boxes
1. Click **Edit > Add Text Box** or use the toolbar button
2. A text box will appear on the canvas
3. Double-click the text box to start editing
4. Type your text
5. Click outside the text box to finish editing

### Selecting Multiple Objects
- **Area Selection**: Click and drag on empty canvas to draw a selection rectangle. All objects within or intersecting the rectangle will be selected
- **Select All**: Press **Ctrl+A** (Windows/Linux) or **Cmd+A** (Mac) to select all objects on the canvas
- **Delete Multiple**: After selecting multiple objects, press **Backspace** or **Delete** to remove them all

### Zooming the Canvas
- **Ctrl/Cmd + Scroll**: Hold **Ctrl** (Windows/Linux) or **Cmd** (Mac) and scroll with your mouse or trackpad to zoom in/out
- **Keyboard Shortcuts**: 
  - Press **+** to zoom in
  - Press **-** to zoom out
  - Press **0** to reset zoom to 100%

### Saving Your Work
1. Click **File > Save Canvas**
2. Choose a location and filename (extension `.fmk` will be added automatically)

### Opening Saved Work
1. Click **File > Open Canvas**
2. Select a previously saved `.fmk` file

## Technical Details

- Built with Java Swing for the GUI
- Uses Gson for JSON serialization of canvas data
- Uses Apache Batik for SVG parsing and element extraction
- Images are embedded in saved files using Base64 encoding
- SVG shapes are converted to native canvas elements for individual editing
- Packaged as a fat JAR using maven-shade-plugin for easy distribution