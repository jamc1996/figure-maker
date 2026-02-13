package com.figuremaker;

import javax.swing.*;

public class FigureMakerApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
