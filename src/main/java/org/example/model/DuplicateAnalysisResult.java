package org.example.model;

import org.example.util.FileUtilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wynik analizy duplikatów plików między katalogiem źródłowym a główną kopią zapasową.
 */
public class DuplicateAnalysisResult {

    private int masterFileCount;
    private final List<BackupFile> sourceFiles = new ArrayList<>();
    private final List<BackupFile> duplicatesInMaster = new ArrayList<>();
    private final List<BackupFile> duplicatesInSource = new ArrayList<>();
    private final List<BackupFile> newFiles = new ArrayList<>();
    private final List<DuplicatePair> duplicatePairs = new ArrayList<>();
    private final Map<String, List<BackupFile>> sourceDuplicateGroups = new HashMap<>();
    private long processingTimeMs;
    private double throughputMbPerSec;

    // ====== SETTERY ======

    public void setMasterFileCount(int count) { this.masterFileCount = count; }
    public void setSourceFiles(List<BackupFile> files) { replaceList(sourceFiles, files); }
    public void setDuplicatesInMaster(List<BackupFile> files) { replaceList(duplicatesInMaster, files); }
    public void setDuplicatesInSource(List<BackupFile> files) { replaceList(duplicatesInSource, files); }
    public void setNewFiles(List<BackupFile> files) { replaceList(newFiles, files); }
    public void setDuplicatePairs(List<DuplicatePair> pairs) { replaceList(duplicatePairs, pairs); }

    public void setSourceDuplicateGroups(Map<String, List<BackupFile>> groups) {
        sourceDuplicateGroups.clear();
        sourceDuplicateGroups.putAll(groups);
    }

    public void setProcessingTimeMs(long time) { this.processingTimeMs = time; }
    public void setThroughputMbPerSec(double throughput) { this.throughputMbPerSec = throughput; }

    // ====== GETTERY ======

    public int getMasterFileCount() { return masterFileCount; }
    public List<BackupFile> getSourceFiles() { return sourceFiles; }
    public List<BackupFile> getDuplicatesInMaster() { return duplicatesInMaster; }
    public List<BackupFile> getDuplicatesInSource() { return duplicatesInSource; }
    public List<DuplicatePair> getDuplicatePairs() { return duplicatePairs; }
    public Map<String, List<BackupFile>> getSourceDuplicateGroups() { return sourceDuplicateGroups; }
    public long getProcessingTimeMs() { return processingTimeMs; }
    public double getThroughputMbPerSec() { return throughputMbPerSec; }

    // ====== STATYSTYKI ======

    public int getTotalSourceFiles() { return sourceFiles.size(); }
    public int getDuplicateInMasterCount() { return duplicatesInMaster.size(); }
    public int getDuplicateInSourceCount() { return duplicatesInSource.size(); }
    public int getTotalDuplicateCount() { return duplicatesInMaster.size() + duplicatesInSource.size(); }
    public int getNewFileCount() { return newFiles.size(); }
    public String getFormattedDuration() { return FileUtilities.formatDuration(processingTimeMs); }

    // ====== POMOCNICZE ======

    private <T> void replaceList(List<T> target, List<T> source) {
        target.clear();
        target.addAll(source);
    }
}

