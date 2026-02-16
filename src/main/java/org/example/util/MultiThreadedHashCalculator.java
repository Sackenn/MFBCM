package org.example.util;

import net.openhft.hashing.LongHashFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

/**
 * Wielowątkowy kalkulator hashy xxHash3 dla plików.
 * Zoptymalizowany dla dużych plików z wykorzystaniem próbkowania fragmentów.
 */
public class MultiThreadedHashCalculator {

    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private static final int SMALL_FILE_THRESHOLD_MB = 100;
    private static final int LARGE_FILE_THRESHOLD_MB = 500;
    private static final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int NUM_CHUNKS = 10;

    private final int threadCount;
    private final ExecutorService executor;
    private final boolean ownsExecutor;

    public MultiThreadedHashCalculator(int threadCount) {
        this.threadCount = Math.max(1, threadCount);

        if (this.threadCount == Runtime.getRuntime().availableProcessors()) {
            this.executor = ForkJoinPool.commonPool();
            this.ownsExecutor = false;
        } else {
            int poolNum = POOL_NUMBER.getAndIncrement();
            AtomicInteger threadNum = new AtomicInteger(1);
            this.executor = Executors.newFixedThreadPool(this.threadCount, r -> {
                Thread t = new Thread(r, "HashCalculator-" + poolNum + "-" + threadNum.getAndIncrement());
                t.setDaemon(false);
                return t;
            });
            this.ownsExecutor = true;
        }
        System.out.println("MultiThreadedHashCalculator initialized with " + this.threadCount + " threads");
    }

    public Map<String, String> calculateHashes(List<File> files,
                                               ProgressCallback progressCallback, BooleanSupplier isCancelled) throws InterruptedException {

        long startTime = System.currentTimeMillis();
        System.out.println("Starting multi-threaded hash calculation of " + files.size() + " files using " + threadCount + " threads");

        Map<String, String> results = new ConcurrentHashMap<>();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger fileIndex = new AtomicInteger(0);
        AtomicInteger progressReportCounter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        int progressBatchSize = Math.max(1, files.size() / 1000);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> processFiles(files, results, fileIndex, completed, errors,
                progressReportCounter, progressBatchSize, progressCallback, isCancelled, latch));
        }

        waitForCompletion(latch, isCancelled);

        long totalTime = System.currentTimeMillis() - startTime;
        logCompletionStats(files, results.size(), errors.get(), totalTime);

        if (progressCallback != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
            double mbPerSecond = calculateThroughput(files, totalTime);
            String timingMessage = "Completed in " + FileUtilities.formatDuration(totalTime) +
                                  " (" + String.format("%.1f", mbPerSecond) + " MB/s)";
            progressCallback.onProgress(results.size(), files.size(), timingMessage, errors.get());
        }

        return results;
    }

    private void processFiles(List<File> files, Map<String, String> results, AtomicInteger fileIndex,
            AtomicInteger completed, AtomicInteger errors, AtomicInteger progressReportCounter,
            int progressBatchSize, ProgressCallback progressCallback, BooleanSupplier isCancelled,
            CountDownLatch latch) {
        try {
            while (true) {
                if (isCancelled != null && isCancelled.getAsBoolean()) break;

                int currentIndex = fileIndex.getAndIncrement();
                if (currentIndex >= files.size()) break;

                File file = files.get(currentIndex);
                processFile(file, results, errors, isCancelled);

                int current = completed.incrementAndGet();
                reportProgress(current, files.size(), file.getName(), errors.get(),
                    progressReportCounter, progressBatchSize, progressCallback, isCancelled);
            }
        } finally {
            latch.countDown();
        }
    }

    private void processFile(File file, Map<String, String> results, AtomicInteger errors, BooleanSupplier isCancelled) {
        try {
            long fileStart = System.currentTimeMillis();
            String hash = calculateFileHash(file);

            if (hash != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
                results.put(file.getAbsolutePath(), hash);
                logLargeFileProcessing(file, fileStart);
            } else if (hash == null) {
                errors.incrementAndGet();
            }
        } catch (Exception e) {
            if (isCancelled == null || !isCancelled.getAsBoolean()) {
                System.err.println("Error hashing file " + file.getAbsolutePath() + ": " + e.getMessage());
                errors.incrementAndGet();
            }
        }
    }

    private void logLargeFileProcessing(File file, long startTime) {
        long largeFileThreshold = LARGE_FILE_THRESHOLD_MB * 1024L * 1024L;
        if (file.length() > largeFileThreshold) {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("Processed large file: " + file.getName() +
                " (" + (file.length() / 1024 / 1024) + "MB) in " + duration + "ms");
        }
    }

    private void reportProgress(int current, int total, String fileName, int errorCount,
            AtomicInteger progressReportCounter, int progressBatchSize,
            ProgressCallback progressCallback, BooleanSupplier isCancelled) {
        if (progressCallback != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
            int reportCount = progressReportCounter.incrementAndGet();
            if (reportCount % progressBatchSize == 0 || current == total) {
                progressCallback.onProgress(current, total, fileName, errorCount);
            }
        }
    }

    private void waitForCompletion(CountDownLatch latch, BooleanSupplier isCancelled) throws InterruptedException {
        if (isCancelled != null) {
            while (latch.getCount() > 0) {
                if (isCancelled.getAsBoolean()) {
                    System.out.println("Cancellation requested, waiting for threads to finish...");
                    break;
                }
                @SuppressWarnings("unused")
                boolean completed = latch.await(100, TimeUnit.MILLISECONDS);
            }
        } else {
            latch.await();
        }
    }

    private void logCompletionStats(List<File> files, int resultCount, int errorCount, long totalTime) {
        double avgTimePerFile = files.isEmpty() ? 0 : (double) totalTime / files.size();
        double mbPerSecond = calculateThroughput(files, totalTime);

        System.out.println("Multi-threaded hash calculation completed:");
        System.out.println("- Files processed: " + resultCount + "/" + files.size());
        System.out.println("- Total time: " + totalTime + "ms");
        System.out.println("- Average per file: " + String.format("%.1f", avgTimePerFile) + "ms");
        System.out.println("- Throughput: " + String.format("%.1f", mbPerSecond) + " MB/s");
        System.out.println("- Threads used: " + threadCount);
        System.out.println("- Errors: " + errorCount);
    }

    private double calculateThroughput(List<File> files, long totalTimeMs) {
        long totalBytes = files.stream().mapToLong(File::length).sum();
        double totalMB = totalBytes / (1024.0 * 1024.0);
        double totalSeconds = totalTimeMs / 1000.0;
        return totalSeconds > 0 ? totalMB / totalSeconds : 0;
    }

    private String calculateFileHash(File file) {
        try {
            LongHashFunction hashFunction = LongHashFunction.xx3();
            long fileSize = file.length();
            long smallFileThreshold = SMALL_FILE_THRESHOLD_MB * 1024L * 1024L;

            if (fileSize < smallFileThreshold) {
                return hashSmallFile(file, hashFunction);
            } else {
                return hashLargeFile(file, hashFunction, fileSize);
            }
        } catch (OutOfMemoryError | IOException e) {
            logHashError(file, e);
            return null;
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR hashing file " + file.getAbsolutePath() +
                " (size: " + (file.length() / 1024 / 1024) + " MB): " + e.getClass().getName() + " - " + e.getMessage());
            return null;
        }
    }

    private String hashSmallFile(File file, LongHashFunction hashFunction) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileBytes = fis.readAllBytes();
            return String.format("%016x", hashFunction.hashBytes(fileBytes));
        }
    }

    private String hashLargeFile(File file, LongHashFunction hashFunction, long fileSize) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            long spacing = fileSize / NUM_CHUNKS;
            byte[] buffer = new byte[CHUNK_SIZE];
            long hash = 0;
            long currentPosition = 0;

            for (int i = 0; i < NUM_CHUNKS; i++) {
                long targetPosition = i * spacing;
                long skipAmount = targetPosition - currentPosition;

                if (skipAmount > 0) {
                    long skipped = fis.skip(skipAmount);
                    currentPosition += skipped;
                }

                int toRead = (int) Math.min(CHUNK_SIZE, fileSize - targetPosition);
                int totalRead = readFully(fis, buffer, toRead);
                currentPosition += totalRead;

                if (totalRead > 0) {
                    long chunkHash = hashFunction.hashBytes(buffer, 0, totalRead);
                    hash = (i == 0) ? chunkHash : Long.rotateLeft(hash, 1) ^ chunkHash;
                }
            }

            return String.format("%016x", hash);
        }
    }

    private int readFully(FileInputStream fis, byte[] buffer, int toRead) throws IOException {
        int totalRead = 0;
        int bytesRead;
        while (totalRead < toRead && (bytesRead = fis.read(buffer, totalRead, toRead - totalRead)) != -1) {
            totalRead += bytesRead;
        }
        return totalRead;
    }

    private void logHashError(File file, Throwable e) {
        String errorType = e instanceof OutOfMemoryError ? "OUT OF MEMORY" : "IO ERROR";
        System.err.println(errorType + " hashing file " + file.getAbsolutePath() +
            " (size: " + (file.length() / 1024 / 1024) + " MB): " + e.getMessage());
    }

    public void shutdown() {
        if (!ownsExecutor) return;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total, String currentFile, int errors);
    }
}
