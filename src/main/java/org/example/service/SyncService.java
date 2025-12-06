package org.example.service;

import org.example.model.BackupConfiguration;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.CancellationException;

/**
 * Service class responsible for synchronizing the master folder to sync locations.
 */
public class SyncService extends SwingWorker<SyncResult, SyncProgress> {

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
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        if (configuration.getMasterBackupLocation() == null) {
            throw new IllegalArgumentException("Master backup location must be set");
        }
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

        // First, calculate total size for progress reporting
        publish(new SyncProgress(0, 1, "Calculating total size...", 0, 0));
        calculateTotalSize(masterLocation);

        // Sync to each location
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
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!file.getFileName().toString().equals(".mfbcm_hashes.json")) {
                    totalFiles++;
                    totalBytes += attrs.size();
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.getFileName().toString().equals(".mfbcm_temp")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void syncDirectory(File source, File target, String locationName) throws IOException {
        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();

        // Create target directory if it doesn't exist
        if (!target.exists()) {
            if (!target.mkdirs()) {
                throw new IOException("Failed to create target directory: " + target.getAbsolutePath());
            }
        }

        // Walk through source directory and copy all files
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                // Skip temp directories and hash files
                if (dir.getFileName().toString().equals(".mfbcm_temp")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                // Create corresponding directory in target
                Path targetDir = targetPath.resolve(sourcePath.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                // Skip hash files
                if (file.getFileName().toString().equals(".mfbcm_hashes.json")) {
                    return FileVisitResult.CONTINUE;
                }

                // Calculate target file path
                Path relativePath = sourcePath.relativize(file);
                Path targetFile = targetPath.resolve(relativePath);

                // Copy file if it doesn't exist or is different
                boolean needsCopy = true;
                if (Files.exists(targetFile)) {
                    // Check if files are different (size and modification time)
                    BasicFileAttributes targetAttrs = Files.readAttributes(targetFile, BasicFileAttributes.class);
                    if (attrs.size() == targetAttrs.size() &&
                        attrs.lastModifiedTime().equals(targetAttrs.lastModifiedTime())) {
                        needsCopy = false;
                    }
                }

                if (needsCopy) {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING,
                              StandardCopyOption.COPY_ATTRIBUTES);
                }

                processedFiles++;
                processedBytes += attrs.size();

                // Publish progress every 10 files or for large files
                if (processedFiles % 10 == 0 || attrs.size() > 10_000_000) {
                    publish(new SyncProgress(processedFiles, totalFiles,
                        locationName + ": " + file.getFileName().toString(),
                        processedBytes, totalBytes));
                }

                return FileVisitResult.CONTINUE;
            }
        });

        // Clean up files in target that don't exist in source
        if (!isCancelled()) {
            cleanupDeletedFiles(sourcePath, targetPath);
        }
    }

    private void cleanupDeletedFiles(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            return;
        }

        Files.walkFileTree(target, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                // Skip hash files
                if (file.getFileName().toString().equals(".mfbcm_hashes.json")) {
                    return FileVisitResult.CONTINUE;
                }

                // Check if this file exists in source
                Path relativePath = target.relativize(file);
                Path sourceFile = source.resolve(relativePath);

                if (!Files.exists(sourceFile)) {
                    Files.delete(file);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                // Skip temp directories
                if (dir.getFileName().toString().equals(".mfbcm_temp")) {
                    return FileVisitResult.CONTINUE;
                }

                // Check if directory exists in source
                Path relativePath = target.relativize(dir);
                Path sourceDir = source.resolve(relativePath);

                if (!Files.exists(sourceDir) && !dir.equals(target)) {
                    // Delete empty directory
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException e) {
                        // Directory not empty, skip
                    }
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    protected void process(List<SyncProgress> chunks) {
        if (progressCallback != null && !chunks.isEmpty()) {
            SyncProgress last = chunks.getLast();
            progressCallback.updateProgress(last.current, last.total,
                last.currentFile, last.bytesProcessed, last.totalBytes);
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
            if (progressCallback != null) {
                progressCallback.syncFailed("Sync was cancelled");
            }
        } catch (Exception e) {
            if (progressCallback != null) {
                progressCallback.syncFailed("Sync failed: " + e.getMessage());
            }
        }
    }
}

