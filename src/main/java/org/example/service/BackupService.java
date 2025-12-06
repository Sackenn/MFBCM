package org.example.service;

import org.example.model.BackupFile;
import org.example.model.BackupConfiguration;
import org.example.util.FileUtilities;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service class responsible for performing the actual backup operations.
 */
public class BackupService extends SwingWorker<Boolean, BackupProgress> {

    private final List<BackupFile> filesToBackup;
    private final BackupConfiguration configuration;
    private final BackupProgressCallback progressCallback;
    private final AtomicInteger successCountBeforeCancellation = new AtomicInteger(0);

    public interface BackupProgressCallback {
        void updateProgress(int current, int total, String currentFile, long bytesProcessed, long totalBytes);
        void backupCompleted(int successCount, int errorCount);
        void backupFailed(String error, int filesBackedUpBeforeCancellation);
        void fileCompleted(BackupFile file, boolean success, String error);
    }

    public BackupService(List<BackupFile> filesToBackup, BackupConfiguration configuration,
                        BackupProgressCallback progressCallback) {
        if (filesToBackup == null) {
            throw new IllegalArgumentException("filesToBackup cannot be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        if (configuration.getMasterBackupLocation() == null) {
            throw new IllegalArgumentException("Master backup location must be set");
        }

        this.filesToBackup = filesToBackup;
        this.configuration = configuration;
        this.progressCallback = progressCallback;
    }

    @Override
    protected Boolean doInBackground() {
        int processedFiles = 0;
        int successCount = 0;
        int errorCount = 0;
        long totalBytes = calculateTotalBytes();
        long processedBytes = 0;

        for (BackupFile backupFile : filesToBackup) {
            if (isCancelled()) {
                // Store the success count before throwing exception
                successCountBeforeCancellation.set(successCount);
                throw new CancellationException("Backup cancelled by user");
            }

            if (!backupFile.isSelected() ||
                backupFile.getStatus() == BackupFile.BackupStatus.DUPLICATE) {
                processedFiles++;
                continue;
            }

            backupFile.setStatus(BackupFile.BackupStatus.IN_PROGRESS);

            try {
                File destinationFile = calculateDestinationPath(backupFile);
                boolean success = copyFile(backupFile.getSourceFile(), destinationFile);

                if (success) {
                    backupFile.setStatus(BackupFile.BackupStatus.COMPLETED);
                    successCount++;
                    processedBytes += backupFile.getSize();
                    notifyFileCompleted(backupFile, true, null);
                } else {
                    backupFile.setStatus(BackupFile.BackupStatus.ERROR);
                    errorCount++;
                    notifyFileCompleted(backupFile, false, "Copy operation failed");
                }

            } catch (Exception e) {
                backupFile.setStatus(BackupFile.BackupStatus.ERROR);
                errorCount++;
                notifyFileCompleted(backupFile, false, e.getMessage());
            }

            processedFiles++;

            // Publish progress
            publish(new BackupProgress(processedFiles, filesToBackup.size(),
                    backupFile.getFileName(), processedBytes, totalBytes));
        }

        // Final callback
        notifyBackupCompleted(successCount, errorCount);
        return true;
    }

    private void notifyFileCompleted(BackupFile file, boolean success, String error) {
        if (progressCallback != null) {
            progressCallback.fileCompleted(file, success, error);
        }
    }

    private void notifyBackupCompleted(int successCount, int errorCount) {
        if (progressCallback != null) {
            progressCallback.backupCompleted(successCount, errorCount);
        }
    }

    private long calculateTotalBytes() {
        return filesToBackup.stream()
                .filter(BackupFile::isSelected)
                .filter(f -> f.getStatus() != BackupFile.BackupStatus.DUPLICATE)
                .mapToLong(BackupFile::getSize)
                .sum();
    }

    private File calculateDestinationPath(BackupFile backupFile) throws IOException {
        File masterLocation = configuration.getMasterBackupLocation();
        String fileName = backupFile.getFileName();

        if (configuration.isCreateDateFolders()) {
            // Create date-based folder structure (YYYY/MM)
            LocalDateTime fileDate = backupFile.getLastModified();
            String yearFolder = String.valueOf(fileDate.getYear());
            String monthFolder = String.format("%02d", fileDate.getMonthValue());

            File dateFolder = new File(masterLocation, yearFolder + File.separator + monthFolder);
            if (!dateFolder.exists() && !dateFolder.mkdirs()) {
                throw new IOException("Failed to create date folder: " + dateFolder.getAbsolutePath());
            }

            return new File(dateFolder, fileName);
        } else {
            return new File(masterLocation, fileName);
        }
    }

    private boolean copyFile(File source, File destination) throws IOException {
        // Check available disk space before copying
        long sourceSize = source.length();
        long availableSpace = destination.getParentFile().getUsableSpace();

        if (availableSpace < sourceSize) {
            throw new IOException("Insufficient disk space. Required: " +
                FileUtilities.formatFileSize(sourceSize) + ", Available: " + FileUtilities.formatFileSize(availableSpace));
        }

        // Handle file name conflicts
        File finalDestination = handleFileNameConflict(destination);

        // Ensure parent directories exist
        File parentDir = finalDestination.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
        }

        // Copy file
        Path sourcePath = source.toPath();
        Path destPath = finalDestination.toPath();

        Files.copy(sourcePath, destPath, StandardCopyOption.COPY_ATTRIBUTES);

        // Verify the copy
        return finalDestination.exists() && finalDestination.length() == source.length();
    }


    private File handleFileNameConflict(File destination) {
        if (!destination.exists()) {
            return destination;
        }

        // If file exists, create a new name with counter
        String fileName = destination.getName();
        String baseName;
        String extension = "";

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = fileName.substring(0, lastDot);
            extension = fileName.substring(lastDot);
        } else {
            baseName = fileName;
        }

        int counter = 1;
        File newDestination;
        do {
            String newFileName = baseName + "_" + counter + extension;
            newDestination = new File(destination.getParent(), newFileName);
            counter++;
        } while (newDestination.exists());

        return newDestination;
    }

    @Override
    protected void process(List<BackupProgress> chunks) {
        if (progressCallback != null && !chunks.isEmpty()) {
            BackupProgress progress = chunks.get(chunks.size() - 1);
            progressCallback.updateProgress(progress.getCurrentFile(), progress.getTotalFiles(),
                    progress.getFileName(), progress.getBytesProcessed(), progress.getTotalBytes());
        }
    }

    @Override
    protected void done() {
        try {
            get(); // This will throw any exceptions that occurred
        } catch (CancellationException e) {
            if (progressCallback != null) {
                progressCallback.backupFailed("Backup was cancelled", successCountBeforeCancellation.get());
            }
        } catch (Exception e) {
            if (progressCallback != null) {
                progressCallback.backupFailed("Backup failed: " + e.getMessage(), 0);
            }
        }
    }
}
