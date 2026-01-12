package org.example.service;

import org.example.model.BackupFile;
import org.example.model.BackupConfiguration;
import org.example.util.MultiThreadedHashCalculator;
import org.example.util.FileUtilities;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serwis wykrywania duplikatów między folderem głównym a katalogami źródłowymi.
 */
public class DuplicateDetectionService extends SwingWorker<DuplicateAnalysisResult, String> {

    private final BackupConfiguration configuration;
    private final HashStorageService hashStorageService;
    private final DuplicateDetectionCallback callback;
    private int totalFiles;
    private int processedFiles;

    public interface DuplicateDetectionCallback {
        void updateProgress(int current, int total, String currentFile);
        void detectionCompleted(DuplicateAnalysisResult result);
        void detectionFailed(String error);
    }

    public DuplicateDetectionService(BackupConfiguration configuration,
                                     HashStorageService hashStorageService,
                                     DuplicateDetectionCallback callback) {
        Objects.requireNonNull(configuration, "configuration cannot be null");
        Objects.requireNonNull(hashStorageService, "hashStorageService cannot be null");
        Objects.requireNonNull(configuration.getMasterBackupLocation(), "Master backup location must be set");

        this.configuration = configuration;
        this.hashStorageService = hashStorageService;
        this.callback = callback;
    }

    @Override
    protected DuplicateAnalysisResult doInBackground() throws Exception {
        validateMasterLocation();

        DuplicateAnalysisResult result = new DuplicateAnalysisResult();
        long startTime = System.currentTimeMillis();

        publish("Loading master folder hashes...");
        Map<String, HashStorageService.FileHashInfo> masterHashes = hashStorageService.getHashToInfoMap();
        result.setMasterFileCount(masterHashes.size());

        publish("Scanning source directories...");
        List<BackupFile> sourceFiles = scanSourceDirectories(masterHashes);

        publish("Analyzing duplicates...");
        analyzeDuplicates(sourceFiles, masterHashes, result);

        captureTimingInfo(result, sourceFiles, startTime);
        return result;
    }

    private void validateMasterLocation() {
        File masterLoc = configuration.getMasterBackupLocation();
        if (masterLoc == null || !masterLoc.exists()) {
            throw new IllegalStateException("Master backup location not set or doesn't exist");
        }
    }

    private void captureTimingInfo(DuplicateAnalysisResult result, List<BackupFile> sourceFiles, long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;
        long totalBytes = sourceFiles.stream().mapToLong(f -> f.getSourceFile().length()).sum();
        double totalMB = totalBytes / (1024.0 * 1024.0);
        double throughput = totalTime > 0 ? totalMB / (totalTime / 1000.0) : 0;

        result.setProcessingTimeMs(totalTime);
        result.setThroughputMbPerSec(throughput);
    }

    // ====== SKANOWANIE KATALOGÓW ŹRÓDŁOWYCH ======

    private List<BackupFile> scanSourceDirectories(Map<String, HashStorageService.FileHashInfo> masterHashes) throws Exception {
        List<File> allSourceFiles = collectSourceFiles();
        totalFiles = allSourceFiles.size();
        processedFiles = 0;

        if (allSourceFiles.isEmpty()) {
            return new ArrayList<>();
        }

        return configuration.isSkipHashing()
            ? scanWithMetadata(allSourceFiles, masterHashes)
            : scanWithHashing(allSourceFiles, masterHashes);
    }

    private List<File> collectSourceFiles() throws CancellationException {
        List<File> allSourceFiles = new ArrayList<>();
        for (File sourceDir : configuration.getSourceDirectories()) {
            if (isCancelled()) {
                throw new CancellationException("Duplicate detection cancelled");
            }
            FileUtilities.collectFilesFromDirectory(sourceDir, allSourceFiles, configuration, this::isCancelled);
        }
        return allSourceFiles;
    }

    private List<BackupFile> scanWithMetadata(List<File> allSourceFiles,
            Map<String, HashStorageService.FileHashInfo> masterHashes) {

        Map<String, HashStorageService.FileHashInfo> masterMetadataMap = buildMasterMetadataMap(masterHashes);
        Map<String, BackupFile> metadataMap = new ConcurrentHashMap<>();
        List<BackupFile> allBackupFiles = new CopyOnWriteArrayList<>();
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(configuration.getHashingThreadCount(),
            r -> {
                Thread t = new Thread(r, "MetadataDuplicateDetector-" + System.currentTimeMillis());
                t.setDaemon(false);
                return t;
            });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (File file : allSourceFiles) {
                futures.add(executor.submit(() ->
                    processFileMetadata(file, masterMetadataMap, metadataMap, allBackupFiles, processedCount)));
            }
            waitForFutures(futures);
        } finally {
            FileUtilities.shutdownExecutor(executor, 5);
        }

        return new ArrayList<>(allBackupFiles);
    }

    private Map<String, HashStorageService.FileHashInfo> buildMasterMetadataMap(
            Map<String, HashStorageService.FileHashInfo> masterHashes) {
        Map<String, HashStorageService.FileHashInfo> masterMetadataMap = new HashMap<>();
        for (HashStorageService.FileHashInfo masterInfo : masterHashes.values()) {
            if (masterInfo != null && masterInfo.getRelativePath() != null) {
                String masterFileName = new File(masterInfo.getRelativePath()).getName();
                String masterKey = masterFileName + "|" + masterInfo.getFileSize();
                masterMetadataMap.put(masterKey, masterInfo);
            }
        }
        return masterMetadataMap;
    }

    private void processFileMetadata(File file, Map<String, HashStorageService.FileHashInfo> masterMetadataMap,
            Map<String, BackupFile> metadataMap, List<BackupFile> allBackupFiles, AtomicInteger processedCount) {
        if (isCancelled()) return;

        try {
            String metadataKey = file.getName() + "|" + file.length();
            BackupFile backupFile = new BackupFile(file, null);

            if (masterMetadataMap.containsKey(metadataKey)) {
                markAsDuplicate(backupFile, true);
            } else if (metadataMap.putIfAbsent(metadataKey, backupFile) != null) {
                markAsDuplicate(backupFile, false);
            }

            allBackupFiles.add(backupFile);
            reportProgress(processedCount.incrementAndGet(), file.getName());
        } catch (Exception e) {
            System.err.println("Error processing file metadata: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    private List<BackupFile> scanWithHashing(List<File> allSourceFiles,
            Map<String, HashStorageService.FileHashInfo> masterHashes) throws InterruptedException {

        List<BackupFile> sourceFiles = new ArrayList<>();
        Set<String> processedHashes = new HashSet<>();

        MultiThreadedHashCalculator calculator = new MultiThreadedHashCalculator(configuration.getHashingThreadCount());
        try {
            Map<String, String> fileHashes = calculator.calculateHashesWithCancellation(
                allSourceFiles, createHashProgressCallback(), this::isCancelled);

            for (File file : allSourceFiles) {
                if (isCancelled()) break;

                String hash = fileHashes.get(file.getAbsolutePath());
                if (hash != null) {
                    BackupFile backupFile = new BackupFile(file, hash);

                    if (masterHashes.containsKey(hash)) {
                        markAsDuplicate(backupFile, true);
                    } else if (processedHashes.contains(hash)) {
                        markAsDuplicate(backupFile, false);
                    } else {
                        processedHashes.add(hash);
                    }

                    sourceFiles.add(backupFile);
                }
            }
        } finally {
            calculator.shutdown();
        }

        return sourceFiles;
    }

    private MultiThreadedHashCalculator.ProgressCallback createHashProgressCallback() {
        return (current, total, currentFile, _) -> {
            processedFiles = current;
            if (!isCancelled()) {
                publish("Analyzing source file: " + currentFile + " (" + current + "/" + total + ")");
                setProgress(Math.min(100, (current * 100) / total));
            }
        };
    }

    private void markAsDuplicate(BackupFile backupFile, boolean existsInMaster) {
        backupFile.setStatus(BackupFile.BackupStatus.DUPLICATE);
        backupFile.setSelected(false);
        backupFile.setExistsInMaster(existsInMaster);
    }

    private void reportProgress(int current, String fileName) {
        if (current % 100 == 0 || current == totalFiles) {
            processedFiles = current;
            publish("Analyzing source file: " + fileName + " (" + current + "/" + totalFiles + ")");
            setProgress(Math.min(100, (current * 100) / totalFiles));
        }
    }

    // ====== ANALIZA DUPLIKATÓW ======

    private void analyzeDuplicates(List<BackupFile> sourceFiles,
                                   Map<String, HashStorageService.FileHashInfo> masterHashes,
                                   DuplicateAnalysisResult result) {
        List<BackupFile> duplicatesInMaster = new ArrayList<>();
        List<BackupFile> duplicatesInSource = new ArrayList<>();
        List<BackupFile> newFiles = new ArrayList<>();
        List<DuplicatePair> duplicatePairs = new ArrayList<>();
        Map<String, List<BackupFile>> sourceDuplicateGroups = new HashMap<>();

        if (configuration.isSkipHashing()) {
            categorizeFilesByStatus(sourceFiles, duplicatesInMaster, duplicatesInSource, newFiles);
        } else {
            analyzeHashedFiles(sourceFiles, masterHashes,
                duplicatesInMaster, duplicatesInSource, newFiles, duplicatePairs, sourceDuplicateGroups);
        }

        result.setSourceFiles(sourceFiles);
        result.setDuplicatesInMaster(duplicatesInMaster);
        result.setDuplicatesInSource(duplicatesInSource);
        result.setNewFiles(newFiles);
        result.setDuplicatePairs(duplicatePairs);
        result.setSourceDuplicateGroups(sourceDuplicateGroups);
    }

    private void categorizeFilesByStatus(List<BackupFile> sourceFiles,
            List<BackupFile> duplicatesInMaster, List<BackupFile> duplicatesInSource, List<BackupFile> newFiles) {
        for (BackupFile file : sourceFiles) {
            if (file.isExistsInMaster()) {
                duplicatesInMaster.add(file);
            } else if (file.getStatus() == BackupFile.BackupStatus.DUPLICATE) {
                duplicatesInSource.add(file);
            } else {
                newFiles.add(file);
            }
        }
    }

    private void analyzeHashedFiles(List<BackupFile> sourceFiles,
            Map<String, HashStorageService.FileHashInfo> masterHashes,
            List<BackupFile> duplicatesInMaster, List<BackupFile> duplicatesInSource, List<BackupFile> newFiles,
            List<DuplicatePair> duplicatePairs, Map<String, List<BackupFile>> sourceDuplicateGroups) {

        for (BackupFile sourceFile : sourceFiles) {
            String hash = sourceFile.getHash();
            if (hash == null) continue;

            if (sourceFile.isExistsInMaster()) {
                duplicatesInMaster.add(sourceFile);
                createDuplicatePair(sourceFile, masterHashes.get(hash), duplicatePairs);
            } else if (sourceFile.getStatus() == BackupFile.BackupStatus.DUPLICATE) {
                duplicatesInSource.add(sourceFile);
                sourceDuplicateGroups.computeIfAbsent(hash, _ -> new ArrayList<>()).add(sourceFile);
            } else {
                newFiles.add(sourceFile);
            }
        }
    }

    private void createDuplicatePair(BackupFile sourceFile, HashStorageService.FileHashInfo masterInfo,
            List<DuplicatePair> duplicatePairs) {
        if (masterInfo == null) return;

        File masterFile = masterInfo.getAbsoluteFile(configuration.getMasterBackupLocation());
        if (masterFile == null) return;

        duplicatePairs.add(new DuplicatePair(sourceFile, masterFile));
    }

    // ====== METODY POMOCNICZE ======


    private void waitForFutures(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            if (isCancelled()) break;
            try {
                future.get();
            } catch (CancellationException | InterruptedException e) {
                // Ignoruj anulowane zadania
            } catch (ExecutionException e) {
                System.err.println("Error in processing: " + e.getMessage());
            }
        }
    }


    @Override
    protected void process(List<String> chunks) {
        if (callback != null && !chunks.isEmpty()) {
            callback.updateProgress(processedFiles, totalFiles, chunks.getLast());
        }
    }

    @Override
    protected void done() {
        try {
            DuplicateAnalysisResult result = get();
            if (callback != null) {
                callback.detectionCompleted(result);
            }
        } catch (CancellationException e) {
            if (callback != null) callback.detectionFailed("Duplicate detection was cancelled");
        } catch (Exception e) {
            if (callback != null) callback.detectionFailed("Duplicate detection failed: " + e.getMessage());
        }
    }
}

