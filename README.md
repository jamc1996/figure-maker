# Figure Maker

A Java desktop application for creating figures for academic publication.

## Features

- **Canvas Management**: Create new canvas or open previously saved canvases
- **Image Support**: Load PNG and JPEG images from your filesystem
- **Image Positioning**: 
  - Drag and drop images to position them on the canvas
  - Right-click on selected images to set exact position (distance from upper left corner)
- **Text Boxes**: Create and edit text boxes on the canvas
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
2. Click **File > New Canvas** to start fresh (or just use the default empty canvas)

### Adding Images
1. Click **Edit > Add Image** or use the toolbar button
2. Select a PNG or JPEG file from your filesystem
3. The image will appear on the canvas at position (50, 50)

### Positioning Images
- **Drag and Drop**: Click and drag an image to move it
- **Manual Positioning**: Right-click on a selected image and enter exact X and Y coordinates

### Adding Text Boxes
1. Click **Edit > Add Text Box** or use the toolbar button
2. A text box will appear on the canvas
3. Double-click the text box to start editing
4. Type your text
5. Click outside the text box to finish editing

### Saving Your Work
1. Click **File > Save Canvas**
2. Choose a location and filename (extension `.fmk` will be added automatically)

### Opening Saved Work
1. Click **File > Open Canvas**
2. Select a previously saved `.fmk` file

## Technical Details

- Built with Java Swing for the GUI
- Uses Gson for JSON serialization of canvas data
- Images are embedded in saved files using Base64 encoding
- Packaged as a fat JAR using maven-shade-plugin for easy distribution