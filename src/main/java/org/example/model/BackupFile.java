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

    public enum BackupStatus { PENDING, IN_PROGRESS, COMPLETED, ERROR, DUPLICATE, UNIQUE }

    private final File sourceFile;
    private final String hash;
    private final long size;
    private final LocalDateTime lastModified;
    private boolean selected = true;
    private BackupStatus status = BackupStatus.PENDING;
    private boolean existsInMaster = false;

    public BackupFile(File sourceFile, String hash) {
        this.sourceFile = Objects.requireNonNull(sourceFile);
        this.hash = hash;
        this.size = sourceFile.length();
        this.lastModified = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(sourceFile.lastModified()), ZoneId.systemDefault());
    }

    // ====== PODSTAWOWE GETTERY ======

    public File getSourceFile() { return sourceFile; }
    public String getFileName() { return sourceFile.getName(); }
    public String getPath() { return sourceFile.getAbsolutePath(); }
    public String getHash() { return hash; }
    public long getSize() { return size; }
    public LocalDateTime getLastModified() { return lastModified; }

    // ====== FORMATOWANIE ======

    public String getFormattedSize() { return FileUtilities.formatFileSize(size); }
    public String getFormattedDate() { return lastModified.format(DATE_FORMATTER); }

    // ====== STAN ======

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }

    public boolean isExistsInMaster() { return existsInMaster; }
    public void setExistsInMaster(boolean existsInMaster) { this.existsInMaster = existsInMaster; }

    // ====== TYP PLIKU ======

    public String getFileExtension() {
        String name = sourceFile.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1).toLowerCase() : "";
    }

    public boolean isVideo() { return VIDEO_EXTENSIONS.contains(getFileExtension()); }
    public boolean isImage() { return !isVideo(); }

    // ====== OBJECT METHODS ======

    @Override
    public String toString() { return getFileName(); }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BackupFile that)) return false;
        return Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() { return Objects.hashCode(hash); }
}
