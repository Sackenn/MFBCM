package org.example.service;

import org.example.model.BackupFile;
import org.example.model.BackupConfiguration;
import org.example.util.MultiThreadedHashCalculator;
import org.example.util.FileUtilities;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.CancellationException;

/**
 * Service class responsible for scanning directories and finding multimedia files.
 * Uses xxHash3 for ultra-fast file hashing with zero-allocation performance.
 */
public class FileScanner extends SwingWorker<List<BackupFile>, String> {


    private final BackupConfiguration configuration;
    private final ScanProgressCallback progressCallback;
    private int totalFiles;
    private int scannedFiles;

    public interface ScanProgressCallback {
        void updateProgress(int current, int total, String currentFile);
        void scanCompleted(List<BackupFile> files);
        void scanFailed(String error);
    }

    public FileScanner(BackupConfiguration configuration, ScanProgressCallback progressCallback) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        if (configuration.getSourceDirectories().isEmpty()) {
            throw new IllegalArgumentException("Source directories cannot be empty");
        }

        this.configuration = configuration;
        this.progressCallback = progressCallback;
    }

    @Override
    protected List<BackupFile> doInBackground() throws Exception {
        long startTime = System.currentTimeMillis();
        List<BackupFile> foundFiles = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        // First pass: collect all multimedia files from all source directories
        publish("Collecting files...");
        List<File> allFiles = new ArrayList<>();
        for (File sourceDir : configuration.getSourceDirectories()) {
            if (isCancelled()) {
                throw new CancellationException("Scan cancelled by user");
            }
            FileUtilities.collectFilesFromDirectory(sourceDir, allFiles, configuration, this::isCancelled);
        }

        totalFiles = allFiles.size();
        scannedFiles = 0;

        if (allFiles.isEmpty()) {
            return foundFiles;
        }

        // Use multi-threaded hash calculation for all collected files
        MultiThreadedHashCalculator calculator = new MultiThreadedHashCalculator(
            configuration.getHashingThreadCount());

        try {
            MultiThreadedHashCalculator.ProgressCallback progressCallback =
                (current, total, currentFile, _) -> {
                    scannedFiles = current;
                    if (!isCancelled()) {
                        publish("Scanning: " + currentFile + " (" + current + "/" + total + ")");
                    }
                };

            Map<String, String> fileHashes = calculator.calculateHashesWithCancellation(
                allFiles, progressCallback, this::isCancelled);

            // Process results and create BackupFile objects
            for (File file : allFiles) {
                if (isCancelled()) {
                    break;
                }

                String hash = fileHashes.get(file.getAbsolutePath());
                if (hash != null) {
                    // Check for duplicates within the scanned files
                    if (seenHashes.contains(hash)) {
                        // Duplicate file found
                        BackupFile backupFile = new BackupFile(file, hash);
                        backupFile.setStatus(BackupFile.BackupStatus.DUPLICATE);
                        backupFile.setSelected(false); // Auto-uncheck duplicates
                        foundFiles.add(backupFile);
                    } else {
                        seenHashes.add(hash);
                        foundFiles.add(new BackupFile(file, hash));
                    }
                }
            }
        } finally {
            calculator.shutdown();
        }

        // Send final timing information
        long totalTime = System.currentTimeMillis() - startTime;
        long totalBytes = foundFiles.stream().mapToLong(f -> f.getSourceFile().length()).sum();
        double totalMB = totalBytes / (1024.0 * 1024.0);
        double throughput = totalTime > 0 ? totalMB / (totalTime / 1000.0) : 0;

        String timeStr = FileUtilities.formatDuration(totalTime);
        String timingMessage = "Completed in " + timeStr + " (" + String.format("%.1f", throughput) + " MB/s)";

        // Send final progress with timing
        if (progressCallback != null) {
            progressCallback.updateProgress(foundFiles.size(), totalFiles, timingMessage);
        }

        return foundFiles;
    }


    @Override
    protected void process(List<String> chunks) {
        if (progressCallback != null && !chunks.isEmpty()) {
            String currentFile = chunks.get(chunks.size() - 1);
            progressCallback.updateProgress(scannedFiles, totalFiles, currentFile);
        }
    }

    @Override
    protected void done() {
        try {
            List<BackupFile> result = get();
            if (progressCallback != null) {
                progressCallback.scanCompleted(result);
            }
        } catch (CancellationException e) {
            if (progressCallback != null) {
                progressCallback.scanFailed("Scan was cancelled");
            }
        } catch (Exception e) {
            if (progressCallback != null) {
                progressCallback.scanFailed("Scan failed: " + e.getMessage());
            }
        }
    }
}
