package org.example.service;

import org.example.model.BackupConfiguration;
import org.example.model.BackupFile;
import org.example.util.FileUtilities;
import org.example.util.MultiThreadedHashCalculator;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serwis skanowania katalogów i znajdowania plików multimedialnych.
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
        this.configuration = Objects.requireNonNull(configuration);
        this.progressCallback = progressCallback;
        if (configuration.getSourceDirectories().isEmpty()) {
            throw new IllegalArgumentException("Source directories cannot be empty");
        }
    }

    @Override
    protected List<BackupFile> doInBackground() throws Exception {
        long startTime = System.currentTimeMillis();

        publish("Collecting files...");
        List<File> allFiles = collectAllFiles();
        totalFiles = allFiles.size();

        if (allFiles.isEmpty()) return List.of();

        List<BackupFile> foundFiles = configuration.isSkipHashing()
            ? scanWithMetadata(allFiles)
            : scanWithHashing(allFiles);

        publishTimingInfo(foundFiles, startTime);
        return foundFiles;
    }

    private List<File> collectAllFiles() {
        List<File> allFiles = new ArrayList<>();
        for (File sourceDir : configuration.getSourceDirectories()) {
            if (isCancelled()) throw new CancellationException("Scan cancelled");
            FileUtilities.collectFilesFromDirectory(sourceDir, allFiles, configuration, this::isCancelled);
        }
        return allFiles;
    }

    // ====== SKANOWANIE BEZ HASZOWANIA ======

    private List<BackupFile> scanWithMetadata(List<File> allFiles) {
        Map<String, BackupFile> metadataMap = new ConcurrentHashMap<>();
        List<BackupFile> allBackupFiles = new CopyOnWriteArrayList<>();
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(configuration.getHashingThreadCount());
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (File file : allFiles) {
                futures.add(executor.submit(() -> processFileMetadata(file, metadataMap, allBackupFiles, processedCount)));
            }
            waitForFutures(futures);
        } finally {
            FileUtilities.shutdownExecutor(executor, 5);
        }

        return new ArrayList<>(allBackupFiles);
    }

    private void processFileMetadata(File file, Map<String, BackupFile> metadataMap,
                                     List<BackupFile> allBackupFiles, AtomicInteger processedCount) {
        if (isCancelled()) return;

        String metadataKey = file.getName() + "|" + file.length();
        BackupFile backupFile = new BackupFile(file, null);

        if (metadataMap.putIfAbsent(metadataKey, backupFile) != null) {
            backupFile.setStatus(BackupFile.BackupStatus.DUPLICATE);
            backupFile.setSelected(false);
        }

        allBackupFiles.add(backupFile);

        int current = processedCount.incrementAndGet();
        if (current % 100 == 0 || current == totalFiles) {
            scannedFiles = current;
            publish("Collecting: " + file.getName() + " (" + current + "/" + totalFiles + ")");
        }
    }

    // ====== SKANOWANIE Z HASZOWANIEM ======

    private List<BackupFile> scanWithHashing(List<File> allFiles) throws InterruptedException {
        List<BackupFile> foundFiles = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();

        MultiThreadedHashCalculator calculator = new MultiThreadedHashCalculator(configuration.getHashingThreadCount());
        try {
            Map<String, String> fileHashes = calculator.calculateHashesWithCancellation(
                allFiles, createProgressCallback(), this::isCancelled);

            for (File file : allFiles) {
                if (isCancelled()) break;

                String hash = fileHashes.get(file.getAbsolutePath());
                if (hash != null) {
                    BackupFile backupFile = new BackupFile(file, hash);

                    if (!seenHashes.add(hash)) {
                        backupFile.setStatus(BackupFile.BackupStatus.DUPLICATE);
                        backupFile.setSelected(false);
                    }

                    foundFiles.add(backupFile);
                }
            }
        } finally {
            calculator.shutdown();
        }

        return foundFiles;
    }

    private MultiThreadedHashCalculator.ProgressCallback createProgressCallback() {
        return (current, total, currentFile, _) -> {
            scannedFiles = current;
            if (!isCancelled()) {
                publish("Scanning: " + currentFile + " (" + current + "/" + total + ")");
            }
        };
    }

    // ====== METODY POMOCNICZE ======

    private void publishTimingInfo(List<BackupFile> foundFiles, long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        long totalBytes = foundFiles.stream().mapToLong(f -> f.getSourceFile().length()).sum();
        double throughput = totalTime > 0 ? (totalBytes / (1024.0 * 1024.0)) / (totalTime / 1000.0) : 0;

        String timingMessage = "Completed in " + FileUtilities.formatDuration(totalTime) +
                              " (" + String.format("%.1f", throughput) + " MB/s)";

        if (progressCallback != null) {
            progressCallback.updateProgress(foundFiles.size(), totalFiles, timingMessage);
        }
    }

    private void waitForFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            if (isCancelled()) break;
            try {
                future.get();
            } catch (CancellationException | InterruptedException | ExecutionException ignored) {}
        }
    }

    @Override
    protected void process(List<String> chunks) {
        if (progressCallback != null && !chunks.isEmpty()) {
            progressCallback.updateProgress(scannedFiles, totalFiles, chunks.getLast());
        }
    }

    @Override
    protected void done() {
        try {
            if (progressCallback != null) progressCallback.scanCompleted(get());
        } catch (CancellationException e) {
            if (progressCallback != null) progressCallback.scanFailed("Scan was cancelled");
        } catch (Exception e) {
            if (progressCallback != null) progressCallback.scanFailed("Scan failed: " + e.getMessage());
        }
    }
}
