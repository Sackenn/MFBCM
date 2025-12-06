package org.example.service;

import org.example.model.BackupFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pair of duplicate files (source and master).
 */
public class DuplicatePair {
    private final BackupFile sourceFile;
    private final File masterFile;
    private final HashStorageService.FileHashInfo masterInfo;
    private final List<File> allDuplicateLocations;

    public DuplicatePair(BackupFile sourceFile, File masterFile, HashStorageService.FileHashInfo masterInfo) {
        this.sourceFile = sourceFile;
        this.masterFile = masterFile;
        this.masterInfo = masterInfo;
        this.allDuplicateLocations = new ArrayList<>();
    }

    public BackupFile getSourceFile() { return sourceFile; }
    public File getMasterFile() { return masterFile; }
    public HashStorageService.FileHashInfo getMasterInfo() { return masterInfo; }

    public String getSourcePath() { return sourceFile.getPath(); }
    public String getMasterPath() { return masterFile.getAbsolutePath(); }
    public String getHash() { return sourceFile.getHash(); }

    public List<File> getAllDuplicateLocations() { return allDuplicateLocations; }
    
    public void setAllDuplicateLocations(List<File> locations) { 
        allDuplicateLocations.clear();
        if (locations != null) {
            allDuplicateLocations.addAll(locations);
        }
    }

    /**
     * Returns a formatted string with all duplicate locations
     */
    public String getFormattedDuplicateLocations() {
        if (allDuplicateLocations.isEmpty()) {
            return "No additional duplicates";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allDuplicateLocations.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(allDuplicateLocations.get(i).getAbsolutePath());
        }
        return sb.toString();
    }

    /**
     * Returns the count of all duplicate locations
     */
    public int getDuplicateCount() {
        return allDuplicateLocations.size();
    }
}
