package org.example.gui;

import org.example.model.BackupFile;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.gui.UIConstants.*;

/**
 * Komponent pokazujący podgląd obrazu jako popup przy najechaniu na plik.
 */
public class ImagePreviewTooltip {

    private static final int MAX_PREVIEW_SIZE = 300;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    // Cache dla miniaturek
    private static final Map<String, ImageIcon> thumbnailCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    // Popup okno
    private static JWindow previewWindow;
    private static JLabel imageLabel;
    private static JLabel infoLabel;
    private static String currentFilePath;

    /**
     * Pokazuje podgląd obrazu w popup.
     */
    public static void showPreview(BackupFile backupFile, Component parent, Point location) {
        if (backupFile == null) {
            hidePreview();
            return;
        }

        File file = backupFile.getSourceFile();
        if (file == null || !file.exists()) {
            hidePreview();
            return;
        }

        // Sprawdź czy to ten sam plik
        if (currentFilePath != null && currentFilePath.equals(file.getAbsolutePath()) && previewWindow != null && previewWindow.isVisible()) {
            return;
        }

        String extension = getFileExtension(file.getName());
        if (!isImageFile(extension)) {
            hidePreview();
            return;
        }

        // Załaduj miniaturkę
        ImageIcon thumbnail = getThumbnail(file);
        if (thumbnail == null) {
            hidePreview();
            return;
        }

        currentFilePath = file.getAbsolutePath();

        // Stwórz lub zaktualizuj okno popup
        if (previewWindow == null) {
            createPreviewWindow();
        }

        imageLabel.setIcon(thumbnail);
        String info = String.format("<html><center><b>%s</b><br/>%s | %s</center></html>",
            escapeHtml(backupFile.getFileName()),
            backupFile.getFormattedSize(),
            backupFile.getFormattedDate()
        );
        infoLabel.setText(info);

        // Pozycja okna
        previewWindow.pack();

        // Oblicz pozycję - obok kursora, ale w obrębie ekranu
        Point screenLocation = new Point(location);
        SwingUtilities.convertPointToScreen(screenLocation, parent);

        // Offset od kursora
        screenLocation.x += 15;
        screenLocation.y += 15;

        // Upewnij się, że mieści się na ekranie
        Rectangle screenBounds = parent.getGraphicsConfiguration().getBounds();
        if (screenLocation.x + previewWindow.getWidth() > screenBounds.x + screenBounds.width) {
            screenLocation.x = screenBounds.x + screenBounds.width - previewWindow.getWidth() - 10;
        }
        if (screenLocation.y + previewWindow.getHeight() > screenBounds.y + screenBounds.height) {
            screenLocation.y = screenBounds.y + screenBounds.height - previewWindow.getHeight() - 10;
        }

        previewWindow.setLocation(screenLocation);
        previewWindow.setVisible(true);
    }

    /**
     * Ukrywa popup z podglądem.
     */
    public static void hidePreview() {
        if (previewWindow != null) {
            previewWindow.setVisible(false);
        }
        currentFilePath = null;
    }

    private static void createPreviewWindow() {
        previewWindow = new JWindow();
        previewWindow.setAlwaysOnTop(true);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBackground(BG_PRIMARY);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(imageLabel, BorderLayout.CENTER);

        infoLabel = new JLabel();
        infoLabel.setFont(FONT_REGULAR);
        infoLabel.setForeground(TEXT_PRIMARY);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(infoLabel, BorderLayout.SOUTH);

        previewWindow.setContentPane(contentPanel);
    }

    private static ImageIcon getThumbnail(File file) {
        String key = file.getAbsolutePath();

        // Sprawdź cache
        if (thumbnailCache.containsKey(key)) {
            return thumbnailCache.get(key);
        }

        // Wyczyść cache jeśli za duży
        if (thumbnailCache.size() > MAX_CACHE_SIZE) {
            thumbnailCache.clear();
        }

        try {
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                return null;
            }

            // Oblicz rozmiar miniaturki zachowując proporcje
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            double scale = Math.min(
                (double) MAX_PREVIEW_SIZE / originalWidth,
                (double) MAX_PREVIEW_SIZE / originalHeight
            );

            // Nie powiększaj małych obrazów
            if (scale > 1.0) {
                scale = 1.0;
            }

            int newWidth = Math.max(1, (int) (originalWidth * scale));
            int newHeight = Math.max(1, (int) (originalHeight * scale));

            // Stwórz miniaturkę
            BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = thumbnail.createGraphics();

            // Wysoka jakość skalowania
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            ImageIcon icon = new ImageIcon(thumbnail);
            thumbnailCache.put(key, icon);
            return icon;
        } catch (Exception e) {
            System.err.println("Error creating thumbnail for: " + file.getName() + " - " + e.getMessage());
            return null;
        }
    }

    private static boolean isImageFile(String extension) {
        return extension != null && IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    /**
     * Czyści cache miniaturek.
     */
    public static void clearCache() {
        thumbnailCache.clear();
    }
}
