package org.example.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration class that holds the backup settings.
 */
public class BackupConfiguration {
    private static final int MAX_THREAD_MULTIPLIER = 2;

    private File masterBackupLocation;
    private final List<File> sourceDirectories;
    private final List<File> syncLocations;
    private boolean includeSubdirectories;
    private boolean createDateFolders;
    private int hashingThreadCount;

    public BackupConfiguration() {
        this.sourceDirectories = new ArrayList<>();
        this.syncLocations = new ArrayList<>();
        this.includeSubdirectories = true;
        this.createDateFolders = false;
        this.hashingThreadCount = Runtime.getRuntime().availableProcessors(); // Use all available cores by default
    }

    // Getters and setters
    public File getMasterBackupLocation() {
        return masterBackupLocation;
    }

    public void setMasterBackupLocation(File masterBackupLocation) {
        this.masterBackupLocation = masterBackupLocation;
    }

    public List<File> getSourceDirectories() {
        return Collections.unmodifiableList(sourceDirectories);
    }

    public void addSourceDirectory(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            if (!sourceDirectories.contains(directory)) {
                sourceDirectories.add(directory);
            }
        }
    }

    public void removeSourceDirectory(File directory) {
        sourceDirectories.remove(directory);
    }

    public List<File> getSyncLocations() {
        return Collections.unmodifiableList(syncLocations);
    }

    public void addSyncLocation(File location) {
        if (location != null && location.exists() && location.isDirectory()) {
            if (!syncLocations.contains(location)) {
                syncLocations.add(location);
            }
        }
    }

    public void removeSyncLocation(File location) {
        syncLocations.remove(location);
    }


    public boolean isIncludeSubdirectories() {
        return includeSubdirectories;
    }

    public void setIncludeSubdirectories(boolean includeSubdirectories) {
        this.includeSubdirectories = includeSubdirectories;
    }


    public boolean isCreateDateFolders() {
        return createDateFolders;
    }

    public void setCreateDateFolders(boolean createDateFolders) {
        this.createDateFolders = createDateFolders;
    }

    public int getHashingThreadCount() {
        return hashingThreadCount;
    }

    public void setHashingThreadCount(int hashingThreadCount) {
        int maxThreads = Runtime.getRuntime().availableProcessors() * MAX_THREAD_MULTIPLIER;
        this.hashingThreadCount = Math.max(1, Math.min(hashingThreadCount, maxThreads));
    }
}
