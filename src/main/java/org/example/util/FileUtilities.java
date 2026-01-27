package org.example.util;

import org.example.model.BackupConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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

    private static final long KB = 1024;
    private static final long MB = KB * 1024;
    private static final long GB = MB * 1024;

    private FileUtilities() {}

    // ====== SPRAWDZANIE PLIKÓW ======

    public static boolean isMultimediaFile(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 && MULTIMEDIA_EXTENSIONS.contains(name.substring(lastDot + 1).toLowerCase());
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
        if (!directory.isDirectory() || (cancelCheck != null && cancelCheck.isCancelled())) return;

        File[] files = directory.listFiles();
        if (files == null) return;

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
        if (milliseconds < 1000) return milliseconds + "ms";
        if (milliseconds < 60000) return String.format("%.1fs", milliseconds / 1000.0);
        return (milliseconds / 60000) + "m " + ((milliseconds % 60000) / 1000) + "s";
    }

    public static String formatFileSize(long bytes) {
        if (bytes < KB) return bytes + " B";
        if (bytes < MB) return String.format("%.1f KB", bytes / (double) KB);
        if (bytes < GB) return String.format("%.1f MB", bytes / (double) MB);
        return String.format("%.2f GB", bytes / (double) GB);
    }

    // ====== EXECUTORY ======

    public static void shutdownExecutor(ExecutorService executor, int timeoutSeconds) {
        if (executor == null) return;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
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
