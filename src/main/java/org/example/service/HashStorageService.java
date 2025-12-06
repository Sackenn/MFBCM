package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import org.example.util.MultiThreadedHashCalculator;
import org.example.util.FileUtilities;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Service for managing xxHash3 hashes of files in the master backup folder.
 * Stores hashes persistently and validates changes on startup.
 * xxHash3 is the latest and fastest variant of xxHash, optimized for modern CPUs and large files.
 */
public class HashStorageService {

    private static final String HASH_FILE_NAME = ".mfbcm_hashes.json";

    private final File masterLocation;
    private final File hashFile;
    private final Map<String, FileHashInfo> storedHashes; // relativePath -> FileHashInfo
    private final Map<String, FileHashInfo> hashToInfoCache; // hash -> FileHashInfo (reverse index for O(1) lookups)
    private final ObjectMapper objectMapper;
    private final int threadCount;


    public HashStorageService(File masterLocation, int threadCount) {
        this.masterLocation = masterLocation;
        this.hashFile = new File(masterLocation, HASH_FILE_NAME);
        this.storedHashes = new ConcurrentHashMap<>();
        this.hashToInfoCache = new ConcurrentHashMap<>(); // Maintains reverse index for fast lookups
        this.objectMapper = new ObjectMapper();
        this.threadCount = Math.max(1, threadCount);

        loadStoredHashes();
    }


    /**
     * Multi-threaded version of validateAndUpdateHashes with progress callback and cancellation support.
     */
    public ValidationResult validateAndUpdateHashesMultiThreaded(
            MultiThreadedHashCalculator.ProgressCallback progressCallback,
            BooleanSupplier isCancelled) throws InterruptedException {

        ValidationResult result = new ValidationResult();

        if (!masterLocation.exists() || !masterLocation.isDirectory()) {
            result.addError("Master location does not exist or is not a directory");
            return result;
        }

        // Get current files in master folder
        Map<String, File> currentFiles = scanMasterFolder();

        if (isCancelled != null && isCancelled.getAsBoolean()) {
            return result;
        }

        // Separate files into new files that need hashing and existing files that need checking
        List<File> filesToHash = new ArrayList<>();

        for (Map.Entry<String, File> entry : currentFiles.entrySet()) {
            String relativePath = entry.getKey();
            File file = entry.getValue();
            FileHashInfo stored = storedHashes.get(relativePath);

            if (stored == null) {
                // New file - needs hashing
                filesToHash.add(file);
            } else {
                // Check if file was modified (quick check without hashing)
                if (file.lastModified() != stored.getLastModified() ||
                    file.length() != stored.getFileSize()) {
                    filesToHash.add(file);
                }
            }
        }

        // Hash files using multi-threaded calculator
        long startTime = System.currentTimeMillis();
        if (!filesToHash.isEmpty()) {
            MultiThreadedHashCalculator calculator = new MultiThreadedHashCalculator(threadCount);
            try {
                Map<String, String> hashedResults = calculator.calculateHashesWithCancellation(
                    filesToHash, progressCallback, isCancelled);

                long hashingTime = System.currentTimeMillis() - startTime;
                long totalBytes = filesToHash.stream().mapToLong(File::length).sum();
                double totalMB = totalBytes / (1024.0 * 1024.0);
                double throughput = hashingTime > 0 ? totalMB / (hashingTime / 1000.0) : 0;

                result.setProcessingTimeMs(hashingTime);
                result.setThroughputMbPerSec(throughput);

                // Update results with hashed files
                for (File file : filesToHash) {
                    if (isCancelled != null && isCancelled.getAsBoolean()) {
                        break;
                    }

                    String hash = hashedResults.get(file.getAbsolutePath());

                    if (hash != null) {
                        String relativePath = getRelativePath(masterLocation, file);
                        FileHashInfo stored = storedHashes.get(relativePath);

                        if (stored == null) {
                            // New file
                            FileHashInfo hashInfo = new FileHashInfo(relativePath, hash,
                                file.lastModified(), file.length());
                            storedHashes.put(relativePath, hashInfo);
                            hashToInfoCache.put(hash, hashInfo); // Update reverse index
                            result.addNewFile(relativePath, hash);
                        } else {
                            // Modified file
                            if (!hash.equals(stored.getHash())) {
                                hashToInfoCache.remove(stored.getHash()); // Remove old hash from cache
                                stored.setHash(hash);
                                stored.setLastModified(file.lastModified());
                                stored.setFileSize(file.length());
                                hashToInfoCache.put(hash, stored); // Add new hash to cache
                                result.addModifiedFile(relativePath, hash);
                            }
                        }
                    }
                }
            } finally {
                calculator.shutdown();
            }
        }

        // Check for deleted files
        if (isCancelled == null || !isCancelled.getAsBoolean()) {
            Set<String> currentPaths = currentFiles.keySet();
            Iterator<Map.Entry<String, FileHashInfo>> iterator = storedHashes.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<String, FileHashInfo> entry = iterator.next();
                String storedPath = entry.getKey();

                if (!currentPaths.contains(storedPath)) {
                    FileHashInfo info = entry.getValue();
                    iterator.remove();
                    hashToInfoCache.remove(info.getHash()); // Update reverse index
                    result.addDeletedFile(storedPath, info.getHash());
                }
            }

            // Save updated hashes
            saveStoredHashes();
        }

        return result;
    }

    /**
     * Gets all stored hashes as a map of hash -> FileHashInfo.
     * Returns the cached map directly for O(1) lookups.
     */
    public Map<String, FileHashInfo> getHashToInfoMap() {
        return new HashMap<>(hashToInfoCache); // Return a defensive copy
    }


    /**
     * Multi-threaded version of forceRehash with progress callback and cancellation support.
     */
    public ValidationResult forceRehashMultiThreaded(
            MultiThreadedHashCalculator.ProgressCallback progressCallback,
            BooleanSupplier isCancelled) throws InterruptedException {
        storedHashes.clear();
        hashToInfoCache.clear(); // Clear the reverse index
        return validateAndUpdateHashesMultiThreaded(progressCallback, isCancelled);
    }

    private Map<String, File> scanMasterFolder() {
        Map<String, File> files = new HashMap<>();
        scanDirectoryRecursive(masterLocation, masterLocation, files);
        return files;
    }

    private void scanDirectoryRecursive(File directory, File baseDirectory, Map<String, File> files) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] dirFiles = directory.listFiles();
        if (dirFiles == null) return;

        for (File file : dirFiles) {
            if (file.isFile() && FileUtilities.isMultimediaFile(file) && !file.getName().equals(HASH_FILE_NAME)) {
                String relativePath = getRelativePath(baseDirectory, file);
                files.put(relativePath, file);
            } else if (file.isDirectory()) {
                scanDirectoryRecursive(file, baseDirectory, files);
            }
        }
    }

    private String getRelativePath(File baseDirectory, File file) {
        Path basePath = baseDirectory.toPath();
        Path filePath = file.toPath();
        return basePath.relativize(filePath).toString().replace('\\', '/');
    }


    private void loadStoredHashes() {
        if (!hashFile.exists()) {
            return;
        }

        try {
            MapType mapType = objectMapper.getTypeFactory()
                .constructMapType(HashMap.class, String.class, FileHashInfo.class);
            Map<String, FileHashInfo> loaded = objectMapper.readValue(hashFile, mapType);

            if (loaded == null) {
                System.err.println("Loaded hash data is null, skipping");
                return;
            }

            // Validate and build the reverse index (hash -> FileHashInfo) for O(1) lookups
            int validCount = 0;
            int invalidCount = 0;

            for (Map.Entry<String, FileHashInfo> entry : loaded.entrySet()) {
                FileHashInfo info = entry.getValue();

                // Validate hash info data
                if (info != null && info.getHash() != null && !info.getHash().isEmpty()
                    && info.getRelativePath() != null && !info.getRelativePath().isEmpty()
                    && info.getFileSize() >= 0 && info.getLastModified() > 0) {

                    storedHashes.put(entry.getKey(), info);
                    hashToInfoCache.put(info.getHash(), info);
                    validCount++;
                } else {
                    invalidCount++;
                }
            }

            if (invalidCount > 0) {
                System.err.println("Warning: Skipped " + invalidCount + " invalid hash entries. Loaded " + validCount + " valid entries.");
            }

        } catch (IOException e) {
            System.err.println("Failed to load stored hashes: " + e.getMessage());
        }
    }

    private void saveStoredHashes() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(hashFile, storedHashes);
        } catch (IOException e) {
            System.err.println("Failed to save hashes: " + e.getMessage());
        }
    }

    /**
     * Information about a file's hash and metadata.
     */
    public static class FileHashInfo {
        private String relativePath;
        private String hash;
        private long lastModified;
        private long fileSize;

        // Default constructor for Jackson deserialization
        public FileHashInfo() {
        }

        public FileHashInfo(String relativePath, String hash, long lastModified, long fileSize) {
            this.relativePath = relativePath;
            this.hash = hash;
            this.lastModified = lastModified;
            this.fileSize = fileSize;
        }

        // Getters and setters
        public String getRelativePath() { return relativePath; }
        public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

        public String getHash() { return hash; }
        public void setHash(String hash) { this.hash = hash; }

        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public File getAbsoluteFile(File masterLocation) {
            return new File(masterLocation, relativePath.replace('/', File.separatorChar));
        }
    }

    /**
     * Result of validation operation.
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final Map<String, String> newFiles = new HashMap<>();
        private final Map<String, String> modifiedFiles = new HashMap<>();
        private final Map<String, String> deletedFiles = new HashMap<>();
        private long processingTimeMs = 0;
        private double throughputMbPerSec = 0.0;

        public void addError(String error) { errors.add(error); }
        public void addNewFile(String path, String hash) { newFiles.put(path, hash); }
        public void addModifiedFile(String path, String hash) { modifiedFiles.put(path, hash); }
        public void addDeletedFile(String path, String hash) { deletedFiles.put(path, hash); }

        public Map<String, String> getNewFiles() { return newFiles; }
        public Map<String, String> getModifiedFiles() { return modifiedFiles; }
        public Map<String, String> getDeletedFiles() { return deletedFiles; }

        public boolean hasChanges() {
            return !newFiles.isEmpty() || !modifiedFiles.isEmpty() || !deletedFiles.isEmpty();
        }

        public int getTotalChanges() {
            return newFiles.size() + modifiedFiles.size() + deletedFiles.size();
        }

        public void setProcessingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public void setThroughputMbPerSec(double throughputMbPerSec) {
            this.throughputMbPerSec = throughputMbPerSec;
        }

        public double getThroughputMbPerSec() {
            return throughputMbPerSec;
        }

        public String getFormattedDuration() {
            return FileUtilities.formatDuration(processingTimeMs);
        }
    }
}
