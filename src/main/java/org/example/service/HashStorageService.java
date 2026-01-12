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
 * Serwis zarządzania haszami xxHash3 plików w folderze głównej kopii zapasowej.
 * Przechowuje hasze trwale i waliduje zmiany przy starcie.
 */
public class HashStorageService {

    private static final String HASH_FILE_NAME = ".mfbcm_hashes.json";

    private final File masterLocation;
    private final File hashFile;
    private final Map<String, FileHashInfo> storedHashes;
    private final Map<String, FileHashInfo> hashToInfoCache;
    private final ObjectMapper objectMapper;
    private final int threadCount;

    public HashStorageService(File masterLocation, int threadCount) {
        this.masterLocation = masterLocation;
        this.hashFile = new File(masterLocation, HASH_FILE_NAME);
        this.storedHashes = new ConcurrentHashMap<>();
        this.hashToInfoCache = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.threadCount = Math.max(1, threadCount);

        loadStoredHashes();
    }

    // ====== PUBLICZNE API ======

    public Map<String, FileHashInfo> getHashToInfoMap() {
        return new HashMap<>(hashToInfoCache);
    }

    public ValidationResult validateAndUpdateHashesMultiThreaded(
            MultiThreadedHashCalculator.ProgressCallback progressCallback,
            BooleanSupplier isCancelled) throws InterruptedException {

        ValidationResult result = new ValidationResult();

        if (!masterLocation.exists() || !masterLocation.isDirectory()) {
            System.err.println("Master location does not exist or is not a directory");
            return result;
        }

        Map<String, File> currentFiles = scanMasterFolder();
        if (isCancelled != null && isCancelled.getAsBoolean()) return result;

        List<File> filesToHash = identifyFilesToHash(currentFiles);

        if (!filesToHash.isEmpty()) {
            processFilesToHash(filesToHash, result, progressCallback, isCancelled);
        }

        if (isCancelled == null || !isCancelled.getAsBoolean()) {
            removeDeletedFiles(currentFiles.keySet(), result);
            saveStoredHashes();
        }

        return result;
    }

    public ValidationResult forceRehashMultiThreaded(
            MultiThreadedHashCalculator.ProgressCallback progressCallback,
            BooleanSupplier isCancelled) throws InterruptedException {
        storedHashes.clear();
        hashToInfoCache.clear();
        return validateAndUpdateHashesMultiThreaded(progressCallback, isCancelled);
    }

    // ====== PRZETWARZANIE PLIKÓW ======

    private List<File> identifyFilesToHash(Map<String, File> currentFiles) {
        List<File> filesToHash = new ArrayList<>();
        for (Map.Entry<String, File> entry : currentFiles.entrySet()) {
            String relativePath = entry.getKey();
            File file = entry.getValue();
            FileHashInfo stored = storedHashes.get(relativePath);

            if (stored == null || isFileModified(file, stored)) {
                filesToHash.add(file);
            }
        }
        return filesToHash;
    }

    private boolean isFileModified(File file, FileHashInfo stored) {
        return file.lastModified() != stored.getLastModified() || file.length() != stored.getFileSize();
    }

    private void processFilesToHash(List<File> filesToHash,
            ValidationResult result, MultiThreadedHashCalculator.ProgressCallback progressCallback,
            BooleanSupplier isCancelled) throws InterruptedException {

        long startTime = System.currentTimeMillis();
        MultiThreadedHashCalculator calculator = new MultiThreadedHashCalculator(threadCount);

        try {
            Map<String, String> hashedResults = calculator.calculateHashesWithCancellation(
                filesToHash, progressCallback, isCancelled);

            updateResultsWithHashes(filesToHash, hashedResults, result, isCancelled);

            long hashingTime = System.currentTimeMillis() - startTime;
            long totalBytes = filesToHash.stream().mapToLong(File::length).sum();
            double totalMB = totalBytes / (1024.0 * 1024.0);
            double throughput = hashingTime > 0 ? totalMB / (hashingTime / 1000.0) : 0;

            result.setProcessingTimeMs(hashingTime);
            result.setThroughputMbPerSec(throughput);
        } finally {
            calculator.shutdown();
        }
    }

    private void updateResultsWithHashes(List<File> filesToHash, Map<String, String> hashedResults,
            ValidationResult result, BooleanSupplier isCancelled) {
        for (File file : filesToHash) {
            if (isCancelled != null && isCancelled.getAsBoolean()) break;

            String hash = hashedResults.get(file.getAbsolutePath());
            if (hash != null) {
                String relativePath = getRelativePath(masterLocation, file);
                FileHashInfo stored = storedHashes.get(relativePath);

                if (stored == null) {
                    addNewFile(relativePath, hash, file, result);
                } else {
                    updateModifiedFile(relativePath, hash, file, stored, result);
                }
            }
        }
    }

    private void addNewFile(String relativePath, String hash, File file, ValidationResult result) {
        FileHashInfo hashInfo = new FileHashInfo(relativePath, hash, file.lastModified(), file.length());
        storedHashes.put(relativePath, hashInfo);
        hashToInfoCache.put(hash, hashInfo);
        result.addNewFile(relativePath, hash);
    }

    private void updateModifiedFile(String relativePath, String hash, File file,
            FileHashInfo stored, ValidationResult result) {
        if (!hash.equals(stored.getHash())) {
            hashToInfoCache.remove(stored.getHash());
            stored.setHash(hash);
            stored.setLastModified(file.lastModified());
            stored.setFileSize(file.length());
            hashToInfoCache.put(hash, stored);
            result.addModifiedFile(relativePath, hash);
        }
    }

    private void removeDeletedFiles(Set<String> currentPaths, ValidationResult result) {
        Iterator<Map.Entry<String, FileHashInfo>> iterator = storedHashes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, FileHashInfo> entry = iterator.next();
            if (!currentPaths.contains(entry.getKey())) {
                FileHashInfo info = entry.getValue();
                iterator.remove();
                hashToInfoCache.remove(info.getHash());
                result.addDeletedFile(entry.getKey(), info.getHash());
            }
        }
    }

    // ====== SKANOWANIE ======

    private Map<String, File> scanMasterFolder() {
        Map<String, File> files = new HashMap<>();
        scanDirectoryRecursive(masterLocation, masterLocation, files);
        return files;
    }

    private void scanDirectoryRecursive(File directory, File baseDirectory, Map<String, File> files) {
        if (!directory.exists() || !directory.isDirectory()) return;

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

    // ====== PERSYSTENCJA ======

    private void loadStoredHashes() {
        if (!hashFile.exists()) return;

        try {
            MapType mapType = objectMapper.getTypeFactory()
                .constructMapType(HashMap.class, String.class, FileHashInfo.class);
            Map<String, FileHashInfo> loaded = objectMapper.readValue(hashFile, mapType);

            if (loaded == null) {
                System.err.println("Loaded hash data is null, skipping");
                return;
            }

            int validCount = 0, invalidCount = 0;
            for (Map.Entry<String, FileHashInfo> entry : loaded.entrySet()) {
                FileHashInfo info = entry.getValue();
                if (isValidHashInfo(info)) {
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

    private boolean isValidHashInfo(FileHashInfo info) {
        return info != null &&
               info.getHash() != null && !info.getHash().isEmpty() &&
               info.getRelativePath() != null && !info.getRelativePath().isEmpty() &&
               info.getFileSize() >= 0 && info.getLastModified() > 0;
    }

    private void saveStoredHashes() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(hashFile, storedHashes);
        } catch (IOException e) {
            System.err.println("Failed to save hashes: " + e.getMessage());
        }
    }

    // ====== KLASY WEWNĘTRZNE ======

    /**
     * Informacje o haszu pliku przechowywane w JSON.
     * Pusty konstruktor i settery są wymagane przez Jackson do deserializacji.
     */
    @SuppressWarnings("unused") // Używane przez Jackson do deserializacji JSON
    public static class FileHashInfo {
        private String relativePath;
        private String hash;
        private long lastModified;
        private long fileSize;

        /** Wymagany przez Jackson do deserializacji */
        public FileHashInfo() {}

        public FileHashInfo(String relativePath, String hash, long lastModified, long fileSize) {
            this.relativePath = relativePath;
            this.hash = hash;
            this.lastModified = lastModified;
            this.fileSize = fileSize;
        }

        public String getRelativePath() { return relativePath; }
        /** Wymagany przez Jackson do deserializacji */
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

    public static class ValidationResult {
        private final Map<String, String> newFiles = new HashMap<>();
        private final Map<String, String> modifiedFiles = new HashMap<>();
        private final Map<String, String> deletedFiles = new HashMap<>();
        private long processingTimeMs = 0;
        private double throughputMbPerSec = 0.0;

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

        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setThroughputMbPerSec(double throughputMbPerSec) { this.throughputMbPerSec = throughputMbPerSec; }
        public double getThroughputMbPerSec() { return throughputMbPerSec; }

        public String getFormattedDuration() { return FileUtilities.formatDuration(processingTimeMs); }
    }
}
