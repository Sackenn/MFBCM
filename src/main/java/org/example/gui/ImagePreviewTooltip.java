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
public final class ImagePreviewTooltip {

    private static final int MAX_PREVIEW_SIZE = 300;
    private static final int MAX_CACHE_SIZE = 100;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    private static final Map<String, ImageIcon> thumbnailCache = new ConcurrentHashMap<>();
    private static JWindow previewWindow;
    private static JLabel imageLabel;
    private static JLabel infoLabel;
    private static String currentFilePath;

    private ImagePreviewTooltip() {}

    public static void showPreview(BackupFile backupFile, Component parent, Point location) {
        if (backupFile == null || backupFile.getSourceFile() == null || !backupFile.getSourceFile().exists()) {
            hidePreview();
            return;
        }

        File file = backupFile.getSourceFile();
        if (currentFilePath != null && currentFilePath.equals(file.getAbsolutePath()) &&
            previewWindow != null && previewWindow.isVisible()) {
            return;
        }

        String extension = getFileExtension(file.getName());
        if (!IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
            hidePreview();
            return;
        }

        ImageIcon thumbnail = getThumbnail(file);
        if (thumbnail == null) {
            hidePreview();
            return;
        }

        currentFilePath = file.getAbsolutePath();

        if (previewWindow == null) createPreviewWindow();

        imageLabel.setIcon(thumbnail);
        infoLabel.setText(String.format("<html><center><b>%s</b><br/>%s | %s</center></html>",
            escapeHtml(backupFile.getFileName()), backupFile.getFormattedSize(), backupFile.getFormattedDate()));

        previewWindow.pack();
        positionWindow(parent, location);
        previewWindow.setVisible(true);
    }

    public static void hidePreview() {
        if (previewWindow != null) previewWindow.setVisible(false);
        currentFilePath = null;
    }

    private static void createPreviewWindow() {
        previewWindow = new JWindow();
        previewWindow.setAlwaysOnTop(true);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.setBackground(BG_PRIMARY);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));

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

    private static void positionWindow(Component parent, Point location) {
        Point screenLocation = new Point(location);
        SwingUtilities.convertPointToScreen(screenLocation, parent);
        screenLocation.translate(15, 15);

        Rectangle screenBounds = parent.getGraphicsConfiguration().getBounds();
        int maxX = screenBounds.x + screenBounds.width - previewWindow.getWidth() - 10;
        int maxY = screenBounds.y + screenBounds.height - previewWindow.getHeight() - 10;

        screenLocation.x = Math.min(screenLocation.x, maxX);
        screenLocation.y = Math.min(screenLocation.y, maxY);

        previewWindow.setLocation(screenLocation);
    }

    private static ImageIcon getThumbnail(File file) {
        String key = file.getAbsolutePath();

        ImageIcon cached = thumbnailCache.get(key);
        if (cached != null) return cached;

        if (thumbnailCache.size() > MAX_CACHE_SIZE) thumbnailCache.clear();

        try {
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) return null;

            double scale = Math.min(1.0, Math.min(
                (double) MAX_PREVIEW_SIZE / originalImage.getWidth(),
                (double) MAX_PREVIEW_SIZE / originalImage.getHeight()));

            int newWidth = Math.max(1, (int) (originalImage.getWidth() * scale));
            int newHeight = Math.max(1, (int) (originalImage.getHeight() * scale));

            BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            ImageIcon icon = new ImageIcon(thumbnail);
            thumbnailCache.put(key, icon);
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private static String escapeHtml(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
