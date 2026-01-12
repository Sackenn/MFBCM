package org.example.util;

import org.example.model.BackupConfiguration;

import java.io.File;
import java.util.*;

/**
 * Narzędzia do operacji na plikach multimedialnych.
 */
public final class FileUtilities {

    private static final Set<String> MULTIMEDIA_EXTENSIONS = Set.of(
        // Zdjęcia
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg",
        "raw", "cr2", "nef", "dng", "arw", "orf", "rw2", "pef", "srw",
        // Video
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp",
        "mpg", "mpeg", "m2v", "mts", "ts", "vob", "asf", "rm", "rmvb"
    );

    private FileUtilities() {
        throw new UnsupportedOperationException("Klasa narzędziowa");
    }

    // ====== SPRAWDZANIE PLIKÓW ======

    public static boolean isMultimediaFile(File file) {
        String name = file.getName().toLowerCase();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return MULTIMEDIA_EXTENSIONS.contains(name.substring(lastDot + 1));
        }
        return false;
    }

    // ====== ZBIERANIE PLIKÓW ======

    public static void collectFilesFromDirectory(File directory, List<File> allFiles,
                                                 BackupConfiguration configuration,
                                                 CancelCheck cancelCheck) {
        collectFilesFromDirectory(directory, allFiles, configuration.isIncludeSubdirectories(), cancelCheck);
    }

    public static void collectFilesFromDirectory(File directory, List<File> allFiles,
                                                 boolean includeSubdirectories,
                                                 CancelCheck cancelCheck) {
        if (!directory.exists() || !directory.isDirectory()) return;
        if (cancelCheck != null && cancelCheck.isCancelled()) return;

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) return;

        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (cancelCheck != null && cancelCheck.isCancelled()) break;

            if (file.isFile() && isMultimediaFile(file)) {
                allFiles.add(file);
            } else if (file.isDirectory() && includeSubdirectories) {
                collectFilesFromDirectory(file, allFiles, true, cancelCheck);
            }
        }
    }

    // ====== FORMATOWANIE ======

    public static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return minutes + "m " + seconds + "s";
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    // ====== EXECUTORY ======

    /**
     * Bezpiecznie zamyka ExecutorService z limitem czasu.
     */
    public static void shutdownExecutor(java.util.concurrent.ExecutorService executor, int timeoutSeconds) {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ====== INTERFEJS FUNKCYJNY ======

    @FunctionalInterface
    public interface CancelCheck {
        boolean isCancelled();
    }
}
