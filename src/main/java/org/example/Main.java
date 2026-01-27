package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import org.example.gui.MainWindow;

import javax.swing.*;
import java.awt.Insets;

public class Main {

    public static void main(String[] args) {
        printStartupInfo();
        initializeLookAndFeel();
        launchApplication();
    }

    private static void printStartupInfo() {
        System.out.println("==============================================");
        System.out.println("Multimedia File Backup Manager");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Available CPUs: " + Runtime.getRuntime().availableProcessors());
        System.out.println("==============================================");
    }

    private static void initializeLookAndFeel() {
        try {
            FlatDarkLaf.setup();
            configureUIDefaults();
        } catch (Exception e) {
            System.err.println("Warning: Could not set FlatLaf Look and Feel: " + e.getMessage());
        }
    }

    private static void configureUIDefaults() {
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.showTabSeparators", true);
    }

    private static void launchApplication() {
        SwingUtilities.invokeLater(() -> {
            try {
                new MainWindow().setVisible(true);
            } catch (Exception e) {
                handleStartupError(e);
            }
        });
    }

    private static void handleStartupError(Exception e) {
        System.err.println("FATAL: Failed to start application: " + e.getMessage());
        JOptionPane.showMessageDialog(null,
            "Failed to start application:\n" + e.getMessage() +
            "\n\nPlease check the console for details.",
            "Startup Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}