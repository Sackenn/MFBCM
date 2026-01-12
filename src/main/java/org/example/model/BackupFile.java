package org.example.model;

import org.example.util.FileUtilities;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

/**
 * Reprezentuje plik multimedialny do kopii zapasowej z metadanymi i stanem.
 */
public class BackupFile {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp",
        "mpg", "mpeg", "m2v", "mts", "ts", "vob", "asf", "rm", "rmvb"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg",
        "raw", "cr2", "nef", "dng", "arw", "orf", "rw2", "pef", "srw"
    );

    public enum BackupStatus {
        PENDING, IN_PROGRESS, COMPLETED, ERROR, DUPLICATE
    }

    private final File sourceFile;
    private final String hash;
    private final long size;
    private final LocalDateTime lastModified;
    private boolean selected = true;
    private BackupStatus status = BackupStatus.PENDING;
    private boolean existsInMaster = false;

    public BackupFile(File sourceFile, String hash) {
        this.sourceFile = sourceFile;
        this.hash = hash;
        this.size = sourceFile.length();
        this.lastModified = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(sourceFile.lastModified()),
            ZoneId.systemDefault()
        );
    }


    public File getSourceFile() { return sourceFile; }
    public String getFileName() { return sourceFile.getName(); }
    public String getPath() { return sourceFile.getAbsolutePath(); }
    public String getHash() { return hash; }
    public long getSize() { return size; }
    public LocalDateTime getLastModified() { return lastModified; }


    public String getFormattedSize() { return FileUtilities.formatFileSize(size); }
    public String getFormattedDate() { return lastModified.format(DATE_FORMATTER); }


    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }

    public boolean isExistsInMaster() { return existsInMaster; }
    public void setExistsInMaster(boolean existsInMaster) { this.existsInMaster = existsInMaster; }


    public String getFileExtension() {
        String name = sourceFile.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "";
    }

    public boolean isVideo() { return VIDEO_EXTENSIONS.contains(getFileExtension()); }
    public boolean isImage() { return IMAGE_EXTENSIONS.contains(getFileExtension()); }


    @Override
    public String toString() { return getFileName(); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BackupFile that = (BackupFile) obj;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() { return hash != null ? hash.hashCode() : 0; }
}
