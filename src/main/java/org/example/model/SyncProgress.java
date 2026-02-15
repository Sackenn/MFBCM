package org.example.model;
/**
 * Postep operacji synchronizacji zawierajacy informacje o aktualnie przetwarzanym pliku.
 */
public record SyncProgress(int current, int total, String currentFile, long bytesProcessed, long totalBytes) {
}
