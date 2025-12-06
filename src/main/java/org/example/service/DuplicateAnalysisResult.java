package org.example.service;

import org.example.model.BackupFile;
import org.example.util.FileUtilities;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result object containing duplicate analysis data.
 */
public class DuplicateAnalysisResult {
    private int masterFileCount;
    private final List<BackupFile> sourceFiles = new ArrayList<>();
    private final List<BackupFile> duplicatesInMaster = new ArrayList<>();
    private final List<BackupFile> duplicatesInSource = new ArrayList<>();
    private final List<BackupFile> newFiles = new ArrayList<>();
    private final List<DuplicatePair> duplicatePairs = new ArrayList<>();
    private final Map<String, List<File>> hashToFilesMap = new HashMap<>(); // Maps hash to all file locations
    private final Map<String, List<BackupFile>> sourceDuplicateGroups = new HashMap<>(); // Maps hash to duplicate files within source
    private long processingTimeMs = 0;
    private double throughputMbPerSec = 0.0;

    // Getters and setters
    public int getMasterFileCount() { return masterFileCount; }
    public void setMasterFileCount(int masterFileCount) { this.masterFileCount = masterFileCount; }

    public List<BackupFile> getSourceFiles() { return sourceFiles; }
    public void setSourceFiles(List<BackupFile> sourceFiles) {
        this.sourceFiles.clear();
        this.sourceFiles.addAll(sourceFiles);
    }

    public List<BackupFile> getDuplicatesInMaster() { return duplicatesInMaster; }
    public void setDuplicatesInMaster(List<BackupFile> duplicatesInMaster) {
        this.duplicatesInMaster.clear();
        this.duplicatesInMaster.addAll(duplicatesInMaster);
    }

    public List<BackupFile> getDuplicatesInSource() { return duplicatesInSource; }
    public void setDuplicatesInSource(List<BackupFile> duplicatesInSource) {
        this.duplicatesInSource.clear();
        this.duplicatesInSource.addAll(duplicatesInSource);
    }

    public List<BackupFile> getNewFiles() { return newFiles; }
    public void setNewFiles(List<BackupFile> newFiles) {
        this.newFiles.clear();
        this.newFiles.addAll(newFiles);
    }

    public List<DuplicatePair> getDuplicatePairs() { return duplicatePairs; }
    public void setDuplicatePairs(List<DuplicatePair> duplicatePairs) {
        this.duplicatePairs.clear();
        this.duplicatePairs.addAll(duplicatePairs);
    }

    public Map<String, List<File>> getHashToFilesMap() { return hashToFilesMap; }
    public void setHashToFilesMap(Map<String, List<File>> hashToFilesMap) {
        this.hashToFilesMap.clear();
        this.hashToFilesMap.putAll(hashToFilesMap);
    }

    public Map<String, List<BackupFile>> getSourceDuplicateGroups() { return sourceDuplicateGroups; }
    public void setSourceDuplicateGroups(Map<String, List<BackupFile>> sourceDuplicateGroups) {
        this.sourceDuplicateGroups.clear();
        this.sourceDuplicateGroups.putAll(sourceDuplicateGroups);
    }

    public int getTotalSourceFiles() { return sourceFiles.size(); }
    public int getDuplicateInMasterCount() { return duplicatesInMaster.size(); }
    public int getDuplicateInSourceCount() { return duplicatesInSource.size(); }
    public int getTotalDuplicateCount() { return duplicatesInMaster.size() + duplicatesInSource.size(); }
    public int getNewFileCount() { return newFiles.size(); }

    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    public long getProcessingTimeMs() { return processingTimeMs; }

    public void setThroughputMbPerSec(double throughputMbPerSec) { this.throughputMbPerSec = throughputMbPerSec; }
    public double getThroughputMbPerSec() { return throughputMbPerSec; }

    public String getFormattedDuration() {
        return FileUtilities.formatDuration(processingTimeMs);
    }
}
