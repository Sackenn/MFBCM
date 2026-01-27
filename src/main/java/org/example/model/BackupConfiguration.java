package org.example.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Konfiguracja kopii zapasowej zawierająca lokalizacje folderów i ustawienia.
 */
public class BackupConfiguration {

    private static final int MAX_THREAD_MULTIPLIER = 2;
    private static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private File masterBackupLocation;
    private final List<File> sourceDirectories = new ArrayList<>();
    private final List<File> syncLocations = new ArrayList<>();
    private boolean includeSubdirectories = true;
    private boolean createDateFolders = false;
    private boolean skipHashing = false;
    private int hashingThreadCount = DEFAULT_THREAD_COUNT;

    // ====== LOKALIZACJA GŁÓWNA ======

    public File getMasterBackupLocation() { return masterBackupLocation; }
    public void setMasterBackupLocation(File location) { this.masterBackupLocation = location; }

    // ====== KATALOGI ŹRÓDŁOWE ======

    public List<File> getSourceDirectories() { return Collections.unmodifiableList(sourceDirectories); }

    public void addSourceDirectory(File directory) {
        if (isValidDirectory(directory) && !sourceDirectories.contains(directory)) {
            sourceDirectories.add(directory);
        }
    }

    public void removeSourceDirectory(File directory) { sourceDirectories.remove(directory); }

    // ====== LOKALIZACJE SYNCHRONIZACJI ======

    public List<File> getSyncLocations() { return Collections.unmodifiableList(syncLocations); }

    public void addSyncLocation(File location) {
        if (isValidDirectory(location) && !syncLocations.contains(location)) {
            syncLocations.add(location);
        }
    }

    public void removeSyncLocation(File location) { syncLocations.remove(location); }

    // ====== OPCJE ======

    public boolean isIncludeSubdirectories() { return includeSubdirectories; }
    public void setIncludeSubdirectories(boolean value) { this.includeSubdirectories = value; }

    public boolean isCreateDateFolders() { return createDateFolders; }
    public void setCreateDateFolders(boolean value) { this.createDateFolders = value; }

    public boolean isSkipHashing() { return skipHashing; }
    public void setSkipHashing(boolean value) { this.skipHashing = value; }

    public int getHashingThreadCount() { return hashingThreadCount; }

    public void setHashingThreadCount(int count) {
        int maxThreads = DEFAULT_THREAD_COUNT * MAX_THREAD_MULTIPLIER;
        this.hashingThreadCount = Math.clamp(count, 1, maxThreads);
    }

    // ====== WALIDACJA ======

    private boolean isValidDirectory(File directory) {
        return directory != null && directory.isDirectory();
    }
}