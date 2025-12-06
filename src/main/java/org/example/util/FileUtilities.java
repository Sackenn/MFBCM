package org.example.util;

import org.example.model.BackupConfiguration;

import java.io.File;
import java.util.*;

/**
 * Utility class containing common file operations and constants used across the application.
 */
public class FileUtilities {

    /**
     * Set of supported multimedia file extensions.
     * Immutable set for thread-safety and performance.
     */
    public static final Set<String> MULTIMEDIA_EXTENSIONS = Set.of(
        // Images
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp", "svg",
        "raw", "cr2", "nef", "dng", "arw", "orf", "rw2", "pef", "srw",
        // Videos
        "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp",
        "mpg", "mpeg", "m2v", "mts", "ts", "vob", "asf", "rm", "rmvb"
    );

    /**
     * Checks if a file is a multimedia file based on its extension.
     *
     * @param file The file to check
     * @return true if the file has a multimedia extension, false otherwise
     */
    public static boolean isMultimediaFile(File file) {
        String name = file.getName().toLowerCase();
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            String extension = name.substring(lastDot + 1);
            return MULTIMEDIA_EXTENSIONS.contains(extension);
        }
        return false;
    }

    /**
     * Recursively collects all multimedia files from a directory.
     *
     * @param directory The directory to scan
     * @param allFiles List to add found files to
     * @param includeSubdirectories Whether to include subdirectories in the scan
     * @param cancelCheck Optional check for cancellation (can be null)
     */
    public static void collectFilesFromDirectory(File directory, List<File> allFiles,
                                                boolean includeSubdirectories,
                                                CancelCheck cancelCheck) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        if (cancelCheck != null && cancelCheck.isCancelled()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        // Only sort if we have files to process
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File file : files) {
            if (cancelCheck != null && cancelCheck.isCancelled()) {
                break;
            }

            if (file.isFile() && isMultimediaFile(file)) {
                allFiles.add(file);
            } else if (file.isDirectory() && includeSubdirectories) {
                collectFilesFromDirectory(file, allFiles, includeSubdirectories, cancelCheck);
            }
        }
    }

    /**
     * Overload that uses BackupConfiguration for convenience.
     */
    public static void collectFilesFromDirectory(File directory, List<File> allFiles,
                                                BackupConfiguration configuration,
                                                CancelCheck cancelCheck) {
        collectFilesFromDirectory(directory, allFiles,
            configuration.isIncludeSubdirectories(), cancelCheck);
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     *
     * @param milliseconds Duration in milliseconds
     * @return Formatted string (e.g., "1m 30s", "45.2s", "234ms")
     */
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

    /**
     * Formats file size to human-readable format.
     *
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "1.5 MB", "234 KB")
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * Functional interface for cancellation checks.
     */
    @FunctionalInterface
    public interface CancelCheck {
        boolean isCancelled();
    }

    // Private constructor to prevent instantiation
    private FileUtilities() {
        throw new UnsupportedOperationException("Utility class");
    }
}

