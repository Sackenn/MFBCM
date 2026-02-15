package org.example.model;

/**
 * Postęp operacji backupu zawierający informacje o aktualnie przetwarzanym pliku.
 */
public record BackupProgress(int currentFile, int totalFiles, String fileName, long bytesProcessed, long totalBytes) {
}

