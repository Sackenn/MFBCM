package org.example.service;

import org.example.model.BackupConfiguration;
import org.example.model.BackupFile;
import org.example.model.BackupProgress;
import org.example.util.FileUtilities;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serwis wykonujący operacje kopii zapasowej.
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
        this.filesToBackup = Objects.requireNonNull(filesToBackup);
        this.configuration = Objects.requireNonNull(configuration);
        this.progressCallback = progressCallback;
        Objects.requireNonNull(configuration.getMasterBackupLocation(), "Master backup location must be set");
    }

    @Override
    protected Boolean doInBackground() {
        int processedFiles = 0, successCount = 0, errorCount = 0;
        long totalBytes = calculateTotalBytes();
        long processedBytes = 0;

        for (BackupFile backupFile : filesToBackup) {
            if (isCancelled()) {
                successCountBeforeCancellation.set(successCount);
                throw new CancellationException("Backup cancelled");
            }

            if (shouldSkipFile(backupFile)) {
                processedFiles++;
                continue;
            }

            backupFile.setStatus(BackupFile.BackupStatus.IN_PROGRESS);

            try {
                File destinationFile = calculateDestinationPath(backupFile);
                if (copyFile(backupFile.getSourceFile(), destinationFile)) {
                    backupFile.setStatus(BackupFile.BackupStatus.COMPLETED);
                    backupFile.setSelected(false);
                    successCount++;
                    processedBytes += backupFile.getSize();
                    notifyFileCompleted(backupFile, true, null);
                } else {
                    markAsError(backupFile, "Copy operation failed");
                    errorCount++;
                }
            } catch (Exception e) {
                markAsError(backupFile, e.getMessage());
                errorCount++;
            }

            processedFiles++;
            publish(new BackupProgress(processedFiles, filesToBackup.size(),
                    backupFile.getFileName(), processedBytes, totalBytes));
        }

        notifyBackupCompleted(successCount, errorCount);
        return true;
    }

    private boolean shouldSkipFile(BackupFile file) {
        return !file.isSelected() || file.getStatus() == BackupFile.BackupStatus.DUPLICATE;
    }

    private void markAsError(BackupFile backupFile, String error) {
        backupFile.setStatus(BackupFile.BackupStatus.ERROR);
        notifyFileCompleted(backupFile, false, error);
    }

    private void notifyFileCompleted(BackupFile file, boolean success, String error) {
        if (progressCallback != null) progressCallback.fileCompleted(file, success, error);
    }

    private void notifyBackupCompleted(int successCount, int errorCount) {
        if (progressCallback != null) progressCallback.backupCompleted(successCount, errorCount);
    }

    private long calculateTotalBytes() {
        return filesToBackup.stream()
                .filter(f -> f.isSelected() && f.getStatus() != BackupFile.BackupStatus.DUPLICATE)
                .mapToLong(BackupFile::getSize)
                .sum();
    }

    // ====== OPERACJE NA PLIKACH ======

    private File calculateDestinationPath(BackupFile backupFile) throws IOException {
        File masterLocation = configuration.getMasterBackupLocation();
        String fileName = backupFile.getFileName();

        if (configuration.isCreateDateFolders()) {
            LocalDateTime fileDate = backupFile.getLastModified();
            File dateFolder = new File(masterLocation,
                fileDate.getYear() + File.separator + String.format("%02d", fileDate.getMonthValue()));

            if (!dateFolder.exists() && !dateFolder.mkdirs()) {
                throw new IOException("Failed to create date folder: " + dateFolder.getAbsolutePath());
            }
            return new File(dateFolder, fileName);
        }
        return new File(masterLocation, fileName);
    }

    private boolean copyFile(File source, File destination) throws IOException {
        validateDiskSpace(source, destination);

        File finalDestination = resolveNameConflict(destination);
        ensureParentDirectoryExists(finalDestination);

        Files.copy(source.toPath(), finalDestination.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        return finalDestination.exists() && finalDestination.length() == source.length();
    }

    private void validateDiskSpace(File source, File destination) throws IOException {
        long sourceSize = source.length();
        long availableSpace = destination.getParentFile().getUsableSpace();

        if (availableSpace < sourceSize) {
            throw new IOException("Insufficient disk space. Required: " +
                FileUtilities.formatFileSize(sourceSize) + ", Available: " + FileUtilities.formatFileSize(availableSpace));
        }
    }

    private void ensureParentDirectoryExists(File file) throws IOException {
        File parentDir = file.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directory: " + parentDir.getAbsolutePath());
        }
    }

    private File resolveNameConflict(File destination) {
        if (!destination.exists()) return destination;

        String fileName = destination.getName();
        int lastDot = fileName.lastIndexOf('.');
        String baseName = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        String extension = lastDot > 0 ? fileName.substring(lastDot) : "";

        int counter = 1;
        File newDestination;
        do {
            newDestination = new File(destination.getParent(), baseName + "_" + counter++ + extension);
        } while (newDestination.exists());

        return newDestination;
    }

    @Override
    protected void process(List<BackupProgress> chunks) {
        if (progressCallback != null && !chunks.isEmpty()) {
            BackupProgress progress = chunks.getLast();
            progressCallback.updateProgress(progress.currentFile(), progress.totalFiles(),
                    progress.fileName(), progress.bytesProcessed(), progress.totalBytes());
        }
    }

    @Override
    protected void done() {
        try {
            get();
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
