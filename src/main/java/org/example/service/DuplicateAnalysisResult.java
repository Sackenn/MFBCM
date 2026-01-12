package org.example.service;

import org.example.model.BackupFile;
import org.example.util.FileUtilities;

import java.util.*;

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
    private long processingTimeMs = 0;
    private double throughputMbPerSec = 0.0;

    // ====== SETTERY Z KOPIOWANIEM ======

    public void setSourceFiles(List<BackupFile> files) { replaceList(sourceFiles, files); }
    public void setDuplicatesInMaster(List<BackupFile> files) { replaceList(duplicatesInMaster, files); }
    public void setDuplicatesInSource(List<BackupFile> files) { replaceList(duplicatesInSource, files); }
    public void setNewFiles(List<BackupFile> files) { replaceList(newFiles, files); }
    public void setDuplicatePairs(List<DuplicatePair> pairs) { replaceList(duplicatePairs, pairs); }

    public void setSourceDuplicateGroups(Map<String, List<BackupFile>> groups) {
        sourceDuplicateGroups.clear();
        sourceDuplicateGroups.putAll(groups);
    }

    private <T> void replaceList(List<T> target, List<T> source) {
        target.clear();
        target.addAll(source);
    }

    // ====== GETTERY ======

    public int getMasterFileCount() { return masterFileCount; }
    public void setMasterFileCount(int count) { this.masterFileCount = count; }

    public List<BackupFile> getSourceFiles() { return sourceFiles; }
    public List<BackupFile> getDuplicatesInMaster() { return duplicatesInMaster; }
    public List<BackupFile> getDuplicatesInSource() { return duplicatesInSource; }
    public List<DuplicatePair> getDuplicatePairs() { return duplicatePairs; }
    public Map<String, List<BackupFile>> getSourceDuplicateGroups() { return sourceDuplicateGroups; }

    // ====== STATYSTYKI ======

    public int getTotalSourceFiles() { return sourceFiles.size(); }
    public int getDuplicateInMasterCount() { return duplicatesInMaster.size(); }
    public int getDuplicateInSourceCount() { return duplicatesInSource.size(); }
    public int getTotalDuplicateCount() { return duplicatesInMaster.size() + duplicatesInSource.size(); }
    public int getNewFileCount() { return newFiles.size(); }

    // ====== CZAS PRZETWARZANIA ======

    public void setProcessingTimeMs(long time) { this.processingTimeMs = time; }
    public long getProcessingTimeMs() { return processingTimeMs; }

    public void setThroughputMbPerSec(double throughput) { this.throughputMbPerSec = throughput; }
    public double getThroughputMbPerSec() { return throughputMbPerSec; }

    public String getFormattedDuration() { return FileUtilities.formatDuration(processingTimeMs); }
}
