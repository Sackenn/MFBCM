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
 * Service class for detecting duplicates between master backup folder and source directories.
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
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        if (hashStorageService == null) {
            throw new IllegalArgumentException("hashStorageService cannot be null");
        }
        if (configuration.getMasterBackupLocation() == null) {
            throw new IllegalArgumentException("Master backup location must be set");
        }

        this.configuration = configuration;
        this.hashStorageService = hashStorageService;
        this.callback = callback;
    }

    @Override
    protected DuplicateAnalysisResult doInBackground() throws Exception {
        if (configuration.getMasterBackupLocation() == null ||
            !configuration.getMasterBackupLocation().exists()) {
            throw new IllegalStateException("Master backup location not set or doesn't exist");
        }

        DuplicateAnalysisResult result = new DuplicateAnalysisResult();
        long startTime = System.currentTimeMillis();

        // Get master folder hashes from storage service
        publish("Loading master folder hashes...");
        Map<String, HashStorageService.FileHashInfo> masterHashes = hashStorageService.getHashToInfoMap();
        result.setMasterFileCount(masterHashes.size());

        // Scan source directories and compare
        publish("Scanning source directories...");
        List<BackupFile> sourceFiles = scanSourceDirectories(masterHashes);

        // Analyze duplicates
        publish("Analyzing duplicates...");
        analyzeDuplicates(sourceFiles, masterHashes, result);

        // Capture timing information
        long totalTime = System.currentTimeMillis() - startTime;
        long totalBytes = sourceFiles.stream().mapToLong(f -> f.getSourceFile().length()).sum();
        double totalMB = totalBytes / (1024.0 * 1024.0);
        double throughput = totalTime > 0 ? totalMB / (totalTime / 1000.0) : 0;

        result.setProcessingTimeMs(totalTime);
        result.setThroughputMbPerSec(throughput);

        return result;
    }

    private List<BackupFile> scanSourceDirectories(Map<String, HashStorageService.FileHashInfo> masterHashes) throws Exception {
        List<BackupFile> sourceFiles = new ArrayList<>();
        Set<String> processedHashes = new HashSet<>();

        // First, collect all multimedia files from source directories
        List<File> allSourceFiles = new ArrayList<>();
        for (File sourceDir : configuration.getSourceDirectories()) {
            if (isCancelled()) {
                throw new CancellationException("Duplicate detection cancelled");
            }
            FileUtilities.collectFilesFromDirectory(sourceDir, allSourceFiles, configuration, this::isCancelled);
        }

        totalFiles = allSourceFiles.size();
        processedFiles = 0;

        if (allSourceFiles.isEmpty()) {
            return sourceFiles;
        }

        // Use multi-threaded hash calculation for all source files
        MultiThreadedHashCalculator calculator = new MultiThreadedHashCalculator(
            configuration.getHashingThreadCount());

        try {
            MultiThreadedHashCalculator.ProgressCallback progressCallback =
                (current, total, currentFile, _) -> {
                    processedFiles = current;
                    if (!isCancelled()) {
                        publish("Analyzing source file: " + currentFile + " (" + current + "/" + total + ")");
                        setProgress(Math.min(100, (current * 100) / total));
                    }
                };

            Map<String, String> fileHashes = calculator.calculateHashesWithCancellation(
                allSourceFiles, progressCallback, this::isCancelled);

            // Process results and create BackupFile objects
            for (File file : allSourceFiles) {
                if (isCancelled()) {
                    break;
                }

                String hash = fileHashes.get(file.getAbsolutePath());
                if (hash != null) {
                    BackupFile backupFile = new BackupFile(file, hash);

                    // Check if duplicate exists in master folder
                    if (masterHashes.containsKey(hash)) {
                        backupFile.setStatus(BackupFile.BackupStatus.DUPLICATE);
                        backupFile.setSelected(false); // Auto-uncheck duplicates
                        backupFile.setExistsInMaster(true);
                    }
                    // Check if duplicate within source files
                    else if (processedHashes.contains(hash)) {
                        backupFile.setStatus(BackupFile.BackupStatus.DUPLICATE);
                        backupFile.setSelected(false); // Auto-uncheck duplicates
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

    private void analyzeDuplicates(List<BackupFile> sourceFiles,
                                 Map<String, HashStorageService.FileHashInfo> masterHashes,
                                 DuplicateAnalysisResult result) {
        List<BackupFile> duplicatesInMaster = new ArrayList<>();
        List<BackupFile> duplicatesInSource = new ArrayList<>();
        List<BackupFile> newFiles = new ArrayList<>();
        List<DuplicatePair> duplicatePairs = new ArrayList<>();
        Map<String, List<File>> hashToFilesMap = new HashMap<>();
        Map<String, List<BackupFile>> sourceDuplicateGroups = new HashMap<>();

        // Build a map of all file locations by hash
        for (BackupFile sourceFile : sourceFiles) {
            String hash = sourceFile.getHash();
            hashToFilesMap.computeIfAbsent(hash, _ -> new ArrayList<>()).add(sourceFile.getSourceFile());
        }

        // Add master files to the map
        for (Map.Entry<String, HashStorageService.FileHashInfo> entry : masterHashes.entrySet()) {
            String hash = entry.getKey();
            File masterFile = entry.getValue().getAbsoluteFile(configuration.getMasterBackupLocation());
            hashToFilesMap.computeIfAbsent(hash, _ -> new ArrayList<>()).add(masterFile);
        }

        for (BackupFile sourceFile : sourceFiles) {
            String hash = sourceFile.getHash();

            if (sourceFile.isExistsInMaster()) {
                // This source file already exists in master backup
                HashStorageService.FileHashInfo masterInfo = masterHashes.get(hash);
                duplicatesInMaster.add(sourceFile);
                DuplicatePair pair = new DuplicatePair(sourceFile, masterInfo.getAbsoluteFile(configuration.getMasterBackupLocation()), masterInfo);

                // Set all duplicate locations for this pair
                List<File> allLocations = hashToFilesMap.get(hash);
                if (allLocations != null) {
                    // Filter out the source file itself and master file from the list
                    List<File> otherLocations = new ArrayList<>();
                    for (File loc : allLocations) {
                        if (!loc.equals(sourceFile.getSourceFile()) && !loc.equals(pair.getMasterFile())) {
                            otherLocations.add(loc);
                        }
                    }
                    pair.setAllDuplicateLocations(otherLocations);
                }

                duplicatePairs.add(pair);
            } else if (sourceFile.getStatus() == BackupFile.BackupStatus.DUPLICATE) {
                // Duplicate within source directories - group them by hash
                duplicatesInSource.add(sourceFile);
                sourceDuplicateGroups.computeIfAbsent(hash, _ -> new ArrayList<>()).add(sourceFile);
            } else {
                // This is a new file
                newFiles.add(sourceFile);
            }
        }

        result.setSourceFiles(sourceFiles);
        result.setDuplicatesInMaster(duplicatesInMaster);
        result.setDuplicatesInSource(duplicatesInSource);
        result.setNewFiles(newFiles);
        result.setDuplicatePairs(duplicatePairs);
        result.setHashToFilesMap(hashToFilesMap);
        result.setSourceDuplicateGroups(sourceDuplicateGroups);
    }


    @Override
    protected void process(List<String> chunks) {
        if (callback != null && !chunks.isEmpty()) {
            String currentFile = chunks.get(chunks.size() - 1);
            callback.updateProgress(processedFiles, totalFiles, currentFile);
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
            if (callback != null) {
                callback.detectionFailed("Duplicate detection was cancelled");
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.detectionFailed("Duplicate detection failed: " + e.getMessage());
            }
        }
    }
}


