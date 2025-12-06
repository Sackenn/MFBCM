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
 * Multi-threaded file hashing utility for calculating xxHash3 hashes of multiple files concurrently.
 * xxHash3 is the latest and fastest variant of xxHash, optimized for modern CPUs.
 */
public class MultiThreadedHashCalculator {

    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);
    private static final int SMALL_FILE_THRESHOLD_MB = 100;
    private static final int SLOW_FILE_THRESHOLD_MS = 5000;
    private static final long LARGE_FILE_THRESHOLD_MB = 500;

    private final int threadCount;
    private final ExecutorService executor;
    private final boolean ownsExecutor; // Track if we created the executor

    public MultiThreadedHashCalculator(int threadCount) {
        this.threadCount = Math.max(1, threadCount);

        // Use ForkJoinPool for better work-stealing on modern CPUs
        // Falls back to fixed thread pool for compatibility
        if (this.threadCount == Runtime.getRuntime().availableProcessors()) {
            // Use common pool when using all CPUs for better efficiency
            this.executor = ForkJoinPool.commonPool();
            this.ownsExecutor = false; // Don't shut down common pool
        } else {
            // Create custom thread pool with meaningful names
            final int poolNum = POOL_NUMBER.getAndIncrement();
            ThreadFactory threadFactory = new ThreadFactory() {
                private final AtomicInteger threadNum = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "HashCalculator-" + poolNum + "-" + threadNum.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            };

            this.executor = Executors.newFixedThreadPool(this.threadCount, threadFactory);
            this.ownsExecutor = true; // We own this pool and must shut it down
        }
        System.out.println("MultiThreadedHashCalculator initialized with " + this.threadCount + " threads");
    }


    /**
     * Calculates xxHash3 hashes for files with cancellation support using an efficient work queue pattern.
     */
    public Map<String, String> calculateHashesWithCancellation(List<File> files,
            ProgressCallback progressCallback, BooleanSupplier isCancelled)
            throws InterruptedException {

        long startTime = System.currentTimeMillis();
        if (files.size() > 100) { // Only log for significant operations
            System.out.println("Starting multi-threaded hash calculation of " + files.size() + " files using " + threadCount + " threads");
        }

        Map<String, String> results = new ConcurrentHashMap<>();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger fileIndex = new AtomicInteger(0);
        AtomicInteger progressReportCounter = new AtomicInteger(0);

        // Use CountDownLatch to wait for all threads to complete
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Determine batch size for progress updates (reduces contention)
        int progressBatchSize = Math.max(1, files.size() / 100); // Update every 1%

        // Create and start worker threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                // Only log thread start for debugging if needed
                // System.out.println("Worker thread " + Thread.currentThread().getName() + " started");

                try {
                    while (true) {
                        // Check for cancellation
                        if (isCancelled != null && isCancelled.getAsBoolean()) {
                            // System.out.println("Thread " + threadName + " cancelled");
                            break;
                        }

                        // Get next file to process
                        int currentIndex = fileIndex.getAndIncrement();
                        if (currentIndex >= files.size()) {
                            break; // No more files to process
                        }

                        File file = files.get(currentIndex);

                        try {
                            long fileStart = System.currentTimeMillis();
                            String hash = calculateFileHash(file);
                            long fileTime = System.currentTimeMillis() - fileStart;

                            if (hash != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
                                results.put(file.getAbsolutePath(), hash);
                                // Only log very slow files (> 5 seconds) or very large files
                                long largeFileThreshold = LARGE_FILE_THRESHOLD_MB * 1024L * 1024L;
                                if (fileTime > SLOW_FILE_THRESHOLD_MS || file.length() > largeFileThreshold) {
                                    System.out.println("Processed large/slow file: " + file.getName() +
                                                     " (" + (file.length()/1024/1024) + "MB) in " + fileTime + "ms");
                                }
                            } else if (hash == null) {
                                errors.incrementAndGet();
                            }
                        } catch (Exception e) {
                            if (isCancelled == null || !isCancelled.getAsBoolean()) {
                                System.err.println("Error hashing file " + file.getAbsolutePath() + ": " + e.getMessage());
                                errors.incrementAndGet();
                            }
                        }

                        // Report progress in batches to reduce contention
                        int current = completed.incrementAndGet();
                        if (progressCallback != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
                            int reportCount = progressReportCounter.incrementAndGet();
                            // Report every batch or on last file
                            if (reportCount % progressBatchSize == 0 || current == files.size()) {
                                progressCallback.onProgress(current, files.size(), file.getName(), errors.get());
                            }
                        }
                    }
                } finally {
                    // System.out.println("Worker thread " + threadName + " finished");
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete or handle cancellation
        if (isCancelled != null) {
            while (latch.getCount() > 0) {
                if (isCancelled.getAsBoolean()) {
                    System.out.println("Cancellation requested, waiting for threads to finish...");
                    break;
                }
                if (!latch.await(100, TimeUnit.MILLISECONDS)) {
                    // Continue waiting
                }
            }
        } else {
            latch.await();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgTimePerFile = files.isEmpty() ? 0 : (double) totalTime / files.size();
        double mbPerSecond = calculateThroughput(files, totalTime);

        System.out.println("Multi-threaded hash calculation completed:");
        System.out.println("- Files processed: " + results.size() + "/" + files.size());
        System.out.println("- Total time: " + totalTime + "ms");
        System.out.println("- Average per file: " + String.format("%.1f", avgTimePerFile) + "ms");
        System.out.println("- Throughput: " + String.format("%.1f", mbPerSecond) + " MB/s");
        System.out.println("- Threads used: " + threadCount);
        System.out.println("- Errors: " + errors.get());

        // Send final progress update with timing information
        if (progressCallback != null && (isCancelled == null || !isCancelled.getAsBoolean())) {
            String timeStr = FileUtilities.formatDuration(totalTime);
            String timingMessage = "Completed in " + timeStr + " (" + String.format("%.1f", mbPerSecond) + " MB/s)";
            progressCallback.onProgress(results.size(), files.size(), timingMessage, errors.get());
        }

        return results;
    }

    /**
     * Calculates throughput in MB/s for performance monitoring.
     */
    private double calculateThroughput(List<File> files, long totalTimeMs) {
        long totalBytes = files.stream().mapToLong(File::length).sum();
        double totalMB = totalBytes / (1024.0 * 1024.0);
        double totalSeconds = totalTimeMs / 1000.0;
        return totalSeconds > 0 ? totalMB / totalSeconds : 0;
    }


    /**
     * Calculates xxHash3 hash for a single file using the most efficient method based on file size.
     * xxHash3 is the latest and fastest variant of xxHash, optimized for modern CPUs.
     * For files > 100MB, reads ten equally spaced 10MB chunks for faster processing.
     */
    private String calculateFileHash(File file) {
        try {
            // Use xxHash3 which is the fastest variant
            LongHashFunction hashFunction = LongHashFunction.xx3();

            long fileSize = file.length();
            long smallFileThreshold = SMALL_FILE_THRESHOLD_MB * 1024L * 1024L;

            // For small to medium files (< 100MB), read into memory
            if (fileSize < smallFileThreshold) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    // Use readAllBytes() for simplicity and correctness
                    byte[] fileBytes = fis.readAllBytes();
                    long hash = hashFunction.hashBytes(fileBytes);
                    return String.format("%016x", hash);
                }
            } else {
                // For large files (>= 100MB), read 10 equally spaced 10MB chunks
                // This provides good uniqueness while being much faster
                try (FileInputStream fis = new FileInputStream(file)) {
                    final int NUM_CHUNKS = 10;
                    final int CHUNK_SIZE = 10 * 1024 * 1024; // 10MB per chunk

                    // Calculate spacing between chunks
                    long spacing = fileSize / NUM_CHUNKS;

                    byte[] buffer = new byte[CHUNK_SIZE];
                    long hash = 0;
                    boolean first = true;

                    for (int i = 0; i < NUM_CHUNKS; i++) {
                        // Calculate position for this chunk
                        long position = i * spacing;

                        // Skip to the position
                        fis.skip(position - (i > 0 ? (i - 1) * spacing + CHUNK_SIZE : 0));

                        // Read up to CHUNK_SIZE bytes (or less if near end of file)
                        int toRead = (int) Math.min(CHUNK_SIZE, fileSize - position);
                        int bytesRead;
                        int totalRead = 0;

                        // Ensure we read the full chunk (or remaining file)
                        while (totalRead < toRead && (bytesRead = fis.read(buffer, totalRead, toRead - totalRead)) != -1) {
                            totalRead += bytesRead;
                        }

                        if (totalRead > 0) {
                            if (first) {
                                hash = hashFunction.hashBytes(buffer, 0, totalRead);
                                first = false;
                            } else {
                                // Combine with rotation for better distribution
                                long chunkHash = hashFunction.hashBytes(buffer, 0, totalRead);
                                hash = Long.rotateLeft(hash, 1) ^ chunkHash;
                            }
                        }
                    }

                    return String.format("%016x", hash);
                }
            }
        } catch (OutOfMemoryError e) {
            System.err.println("OUT OF MEMORY hashing file " + file.getAbsolutePath() +
                             " (size: " + (file.length() / 1024 / 1024) + " MB): " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("IO ERROR hashing file " + file.getAbsolutePath() +
                             " (size: " + (file.length() / 1024 / 1024) + " MB): " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("UNEXPECTED ERROR hashing file " + file.getAbsolutePath() +
                             " (size: " + (file.length() / 1024 / 1024) + " MB): " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Shuts down the thread pool. Call this when done with the calculator.
     * Note: Does not shut down ForkJoinPool.commonPool() as it's shared.
     */
    public void shutdown() {
        if (!ownsExecutor) {
            // Don't shut down common pool - it's shared
            return;
        }

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


    /**
     * Callback interface for progress reporting.
     */
    public interface ProgressCallback {
        /**
         * Called when progress is made.
         *
         * @param current Number of files processed
         * @param total Total number of files
         * @param currentFile Name of current file being processed
         * @param errors Number of errors encountered so far
         */
        void onProgress(int current, int total, String currentFile, int errors);
    }
}
