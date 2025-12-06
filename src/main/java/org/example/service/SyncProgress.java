package org.example.service;

/**
 * Progress information for sync operations.
 */
public class SyncProgress {
    final int current;
    final int total;
    final String currentFile;
    final long bytesProcessed;
    final long totalBytes;

    public SyncProgress(int current, int total, String currentFile, long bytesProcessed, long totalBytes) {
        this.current = current;
        this.total = total;
        this.currentFile = currentFile;
        this.bytesProcessed = bytesProcessed;
        this.totalBytes = totalBytes;
    }

    public int getCurrent() {
        return current;
    }

    public int getTotal() {
        return total;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public long getBytesProcessed() {
        return bytesProcessed;
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}

