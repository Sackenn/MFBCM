package org.example.model;

import org.example.util.FileUtilities;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Represents a multimedia file that can be backed up.
 */
public class BackupFile {
    // Cached regex patterns for performance
    private static final Pattern VIDEO_PATTERN = Pattern.compile("mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|mpg|mpeg|m2v|mts|ts|vob|asf|rm|rmvb");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("jpg|jpeg|png|gif|bmp|tiff|tif|webp|svg|raw|cr2|nef|dng|arw|orf|rw2|pef|srw");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File sourceFile;
    private final String hash;
    private final long size;
    private final LocalDateTime lastModified;
    private boolean selected;
    private BackupStatus status;
    private boolean existsInMaster; // Flag to indicate if this file already exists in master

    public enum BackupStatus {
        PENDING, IN_PROGRESS, COMPLETED, ERROR, DUPLICATE
    }

    public BackupFile(File sourceFile, String hash) {
        this.sourceFile = sourceFile;
        this.hash = hash;
        this.size = sourceFile.length();
        this.lastModified = LocalDateTime.ofEpochSecond(
            sourceFile.lastModified() / 1000, 0,
            java.time.ZoneOffset.systemDefault().getRules().getOffset(java.time.Instant.now())
        );
        this.selected = true; // Default to selected
        this.status = BackupStatus.PENDING;
    }

    // Getters and setters
    public File getSourceFile() {
        return sourceFile;
    }

    public String getFileName() {
        return sourceFile.getName();
    }

    public String getPath() {
        return sourceFile.getAbsolutePath();
    }

    public String getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public String getFormattedSize() {
        return FileUtilities.formatFileSize(size);
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getFormattedDate() {
        return lastModified.format(DATE_FORMATTER);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }


    public boolean isExistsInMaster() {
        return existsInMaster;
    }

    public void setExistsInMaster(boolean existsInMaster) {
        this.existsInMaster = existsInMaster;
    }

    public String getFileExtension() {
        String name = sourceFile.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "";
    }

    public boolean isVideo() {
        String ext = getFileExtension();
        return VIDEO_PATTERN.matcher(ext).matches();
    }

    public boolean isImage() {
        String ext = getFileExtension();
        return IMAGE_PATTERN.matcher(ext).matches();
    }

    @Override
    public String toString() {
        return getFileName();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BackupFile that = (BackupFile) obj;
        return hash != null ? hash.equals(that.hash) : that.hash == null;
    }

    @Override
    public int hashCode() {
        return hash != null ? hash.hashCode() : 0;
    }
}
