package org.example.service;

public record BackupProgress(int currentFile, int totalFiles, String fileName, long bytesProcessed, long totalBytes) {
}

