package org.example.service;

import org.example.model.BackupConfiguration;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Serwis synchronizacji folderu głównego do lokalizacji synchronizacji.
 */
public class SyncService extends SwingWorker<SyncResult, SyncProgress> {

    private static final String HASH_FILE_NAME = ".mfbcm_hashes.json";
    private static final String TEMP_DIR_NAME = ".mfbcm_temp";

    private final BackupConfiguration configuration;
    private final SyncProgressCallback progressCallback;
    private long totalBytes = 0;
    private long processedBytes = 0;
    private int totalFiles = 0;
    private int processedFiles = 0;

    public interface SyncProgressCallback {
        void updateProgress(int current, int total, String currentFile, long bytesProcessed, long totalBytes);
        void syncCompleted(SyncResult result);
        void syncFailed(String error);
    }

    public SyncService(BackupConfiguration configuration, SyncProgressCallback progressCallback) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        Objects.requireNonNull(configuration.getMasterBackupLocation(), "Master backup location must be set");
        if (configuration.getSyncLocations().isEmpty()) {
            throw new IllegalArgumentException("At least one sync location must be set");
        }

        this.configuration = configuration;
        this.progressCallback = progressCallback;
    }

    @Override
    protected SyncResult doInBackground() throws Exception {
        SyncResult result = new SyncResult();
        File masterLocation = configuration.getMasterBackupLocation();

        publish(new SyncProgress(0, 1, "Calculating total size...", 0, 0));
        calculateTotalSize(masterLocation);

        for (File syncLocation : configuration.getSyncLocations()) {
            if (isCancelled()) {
                throw new CancellationException("Sync cancelled by user");
            }

            try {
                publish(new SyncProgress(processedFiles, totalFiles,
                    "Syncing to: " + syncLocation.getName(), processedBytes, totalBytes));
                syncDirectory(masterLocation, syncLocation, syncLocation.getName());
                result.addSuccessfulLocation(syncLocation);
            } catch (Exception e) {
                result.addFailedLocation(syncLocation, e.getMessage());
            }
        }

        return result;
    }

    private void calculateTotalSize(File directory) throws IOException {
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!isSystemFile(file)) {
                    totalFiles++;
                    totalBytes += attrs.size();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return isSystemDirectory(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }
        });
    }

    private void syncDirectory(File source, File target, String locationName) throws IOException {
        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();

        if (!target.exists() && !target.mkdirs()) {
            throw new IOException("Failed to create target directory: " + target.getAbsolutePath());
        }

        copyFilesToTarget(sourcePath, targetPath, locationName);

        if (!isCancelled()) {
            cleanupDeletedFiles(sourcePath, targetPath);
        }
    }

    private void copyFilesToTarget(Path sourcePath, Path targetPath, String locationName) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) return FileVisitResult.TERMINATE;
                if (isSystemDirectory(dir)) return FileVisitResult.SKIP_SUBTREE;

                Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) return FileVisitResult.TERMINATE;
                if (isSystemFile(file)) return FileVisitResult.CONTINUE;

                Path targetFile = targetPath.resolve(sourcePath.relativize(file));
                copyIfNeeded(file, targetFile, attrs);

                processedFiles++;
                processedBytes += attrs.size();

                if (processedFiles % 10 == 0 || attrs.size() > 10_000_000) {
                    publish(new SyncProgress(processedFiles, totalFiles,
                        locationName + ": " + file.getFileName(), processedBytes, totalBytes));
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyIfNeeded(Path source, Path target, BasicFileAttributes sourceAttrs) throws IOException {
        if (Files.exists(target)) {
            BasicFileAttributes targetAttrs = Files.readAttributes(target, BasicFileAttributes.class);
            if (sourceAttrs.size() == targetAttrs.size() &&
                sourceAttrs.lastModifiedTime().equals(targetAttrs.lastModifiedTime())) {
                return; // Plik jest identyczny
            }
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void cleanupDeletedFiles(Path source, Path target) throws IOException {
        if (!Files.exists(target)) return;

        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) return FileVisitResult.TERMINATE;
                if (isSystemFile(file)) return FileVisitResult.CONTINUE;

                Path sourceFile = source.resolve(target.relativize(file));
                if (!Files.exists(sourceFile)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (isCancelled()) return FileVisitResult.TERMINATE;
                if (isSystemDirectory(dir)) return FileVisitResult.CONTINUE;

                Path sourceDir = source.resolve(target.relativize(dir));
                if (!Files.exists(sourceDir) && !dir.equals(target)) {
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException e) {
                        // Katalog nie jest pusty, pomiń
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isSystemFile(Path file) {
        return file.getFileName().toString().equals(HASH_FILE_NAME);
    }

    private boolean isSystemDirectory(Path dir) {
        return dir.getFileName().toString().equals(TEMP_DIR_NAME);
    }

    @Override
    protected void process(List<SyncProgress> chunks) {
        if (progressCallback != null && !chunks.isEmpty()) {
            SyncProgress last = chunks.getLast();
            progressCallback.updateProgress(last.current(), last.total(),
                    last.currentFile(), last.bytesProcessed(), last.totalBytes());
        }
    }

    @Override
    protected void done() {
        try {
            SyncResult result = get();
            if (progressCallback != null) {
                progressCallback.syncCompleted(result);
            }
        } catch (CancellationException e) {
            if (progressCallback != null) progressCallback.syncFailed("Sync was cancelled");
        } catch (Exception e) {
            if (progressCallback != null) progressCallback.syncFailed("Sync failed: " + e.getMessage());
        }
    }
}
