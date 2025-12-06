package org.example;

import org.example.gui.MainWindow;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

/**
 * Main class for the Multimedia File Backup Manager application.
 */
public class Main {
    private static final String APP_VERSION = "1.0-SNAPSHOT";
    private static final String APP_NAME = "Multimedia File Backup Manager";

    public static void main(String[] args) {
        // Print startup banner
        System.out.println("==============================================");
        System.out.println(APP_NAME + " v" + APP_VERSION);
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Available CPUs: " + Runtime.getRuntime().availableProcessors());
        System.out.println("==============================================");

        // Set modern dark theme using FlatLaf
        try {
            FlatDarkLaf.setup();
            // Customize FlatLaf properties for modern look
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.thumbArc", 8);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.showTabSeparators", true);
        } catch (Exception e) {
            System.err.println("Warning: Could not set FlatLaf Look and Feel: " + e.getMessage());
            // Continue with default Look and Feel
        }

        // Launch GUI on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow mainWindow = new MainWindow();
                mainWindow.setVisible(true);
            } catch (Exception e) {
                // Log error to console with full stack trace
                System.err.println("FATAL: Failed to start application: " + e.getMessage());
                e.printStackTrace();

                // Show user-friendly error dialog
                JOptionPane.showMessageDialog(null,
                    "Failed to start application:\n" + e.getMessage() +
                    "\n\nPlease check the console for details.",
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}