package org.example.service;

public record SyncProgress(int current, int total, String currentFile, long bytesProcessed, long totalBytes) {
}

