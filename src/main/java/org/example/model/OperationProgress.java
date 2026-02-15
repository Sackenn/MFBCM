package org.example.model;

/**
 * Uniwersalny rekord postępu operacji (backup, sync, skanowanie).
 * Zawiera informacje o aktualnie przetwarzanym pliku i postępie bajtowym.
 */
public record OperationProgress(
    int currentFile,
    int totalFiles,
    String fileName,
    long bytesProcessed,
    long totalBytes
){}