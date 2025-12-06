package org.example.service;

/**
 * Helper class to carry backup progress information for SwingWorker.
 */
public class BackupProgress {
    private final int currentFile;
    private final int totalFiles;
    private final String fileName;
    private final long bytesProcessed;
    private final long totalBytes;

    public BackupProgress(int currentFile, int totalFiles, String fileName,
                          long bytesProcessed, long totalBytes) {
        this.currentFile = currentFile;
        this.totalFiles = totalFiles;
        this.fileName = fileName;
        this.bytesProcessed = bytesProcessed;
        this.totalBytes = totalBytes;
    }

    // Getters
    public int getCurrentFile() { return currentFile; }
    public int getTotalFiles() { return totalFiles; }
    public String getFileName() { return fileName; }
    public long getBytesProcessed() { return bytesProcessed; }
    public long getTotalBytes() { return totalBytes; }
}

