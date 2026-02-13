package com.figuremaker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MainWindow extends JFrame {
    private FigureCanvas canvas;
    
    public MainWindow() {
        setTitle("Figure Maker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        initializeUI();
    }
    
    private void initializeUI() {
        // Create menu bar
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem newCanvasItem = new JMenuItem("New Canvas");
        newCanvasItem.addActionListener(e -> newCanvas());
        
        JMenuItem openCanvasItem = new JMenuItem("Open Canvas");
        openCanvasItem.addActionListener(e -> openCanvas());
        
        JMenuItem saveCanvasItem = new JMenuItem("Save Canvas");
        saveCanvasItem.addActionListener(e -> saveCanvas());
        saveCanvasItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, 
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(newCanvasItem);
        fileMenu.add(openCanvasItem);
        fileMenu.add(saveCanvasItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        
        JMenuItem addImageItem = new JMenuItem("Add Image");
        addImageItem.addActionListener(e -> addImage());
        
        JMenuItem addTextBoxItem = new JMenuItem("Add Text Box");
        addTextBoxItem.addActionListener(e -> addTextBox());
        
        editMenu.add(addImageItem);
        editMenu.add(addTextBoxItem);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        setJMenuBar(menuBar);
        
        // Create canvas
        canvas = new FigureCanvas();
        
        // Add canvas to center
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        
        // Create toolbar
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton addImageBtn = new JButton("Add Image");
        addImageBtn.addActionListener(e -> addImage());
        
        JButton addTextBtn = new JButton("Add Text Box");
        addTextBtn.addActionListener(e -> addTextBox());
        
        toolbar.add(addImageBtn);
        toolbar.add(addTextBtn);
        
        return toolbar;
    }
    
    private void newCanvas() {
        int result = JOptionPane.showConfirmDialog(this, 
            "Create a new canvas? Any unsaved changes will be lost.",
            "New Canvas", JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            canvas.clear();
        }
    }
    
    private void openCanvas() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Figure Maker Files (*.fmk)", "fmk"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                canvas.loadFromFile(file);
                JOptionPane.showMessageDialog(this, "Canvas loaded successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error loading canvas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void saveCanvas() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Figure Maker Files (*.fmk)", "fmk"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".fmk")) {
                file = new File(file.getAbsolutePath() + ".fmk");
            }
            
            try {
                canvas.saveToFile(file);
                JOptionPane.showMessageDialog(this, "Canvas saved successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error saving canvas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void addImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image Files (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            canvas.addImage(file);
        }
    }
    
    private void addTextBox() {
        canvas.addTextBox();
    }
}
