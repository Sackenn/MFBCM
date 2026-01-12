package org.example.service;

import org.example.model.BackupFile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Serwis usuwania plików z obsługą postępu i anulowania.
 */
public class FileDeleteService extends SwingWorker<FileDeleteService.DeleteResult, String> {

    private final List<BackupFile> filesToDelete;
    private final DeleteProgressCallback callback;

    public interface DeleteProgressCallback {
        void updateProgress(int current, int total, String currentFile);
        void deleteCompleted(DeleteResult result);
        void deleteFailed(String error);
    }

    public FileDeleteService(List<BackupFile> filesToDelete, DeleteProgressCallback callback) {
        Objects.requireNonNull(filesToDelete, "filesToDelete cannot be null");
        this.filesToDelete = new ArrayList<>(filesToDelete);
        this.callback = callback;
    }

    @Override
    protected DeleteResult doInBackground() {
        DeleteResult result = new DeleteResult();
        int total = filesToDelete.size();

        for (int i = 0; i < filesToDelete.size(); i++) {
            if (isCancelled()) {
                break;
            }

            BackupFile file = filesToDelete.get(i);
            publish(file.getFileName());

            try {
                if (file.getSourceFile().delete()) {
                    result.addDeleted(file);
                } else {
                    result.addFailed(file, "Could not delete file (may be in use or protected)");
                }
            } catch (SecurityException e) {
                result.addFailed(file, "Access denied: " + e.getMessage());
            } catch (Exception e) {
                result.addFailed(file, e.getMessage());
            }

            setProgress((i + 1) * 100 / total);
        }

        return result;
    }

    @Override
    protected void process(List<String> chunks) {
        if (callback != null && !chunks.isEmpty()) {
            int current = (int) (getProgress() * filesToDelete.size() / 100.0);
            callback.updateProgress(current, filesToDelete.size(), chunks.getLast());
        }
    }

    @Override
    protected void done() {
        try {
            DeleteResult result = get();
            if (callback != null) {
                callback.deleteCompleted(result);
            }
        } catch (java.util.concurrent.CancellationException e) {
            if (callback != null) {
                callback.deleteFailed("Delete operation was cancelled");
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.deleteFailed("Delete failed: " + e.getMessage());
            }
        }
    }

    /**
     * Wynik operacji usuwania plików.
     */
    public static class DeleteResult {
        private final List<BackupFile> deletedFiles = new ArrayList<>();
        private final List<FailedFile> failedFiles = new ArrayList<>();

        public void addDeleted(BackupFile file) {
            deletedFiles.add(file);
        }

        public void addFailed(BackupFile file, String reason) {
            failedFiles.add(new FailedFile(file, reason));
        }

        public List<BackupFile> getDeletedFiles() { return deletedFiles; }
        public List<FailedFile> getFailedFiles() { return failedFiles; }
        public int getDeletedCount() { return deletedFiles.size(); }
        public int getFailedCount() { return failedFiles.size(); }
        public boolean hasFailures() { return !failedFiles.isEmpty(); }

        public long getTotalDeletedSize() {
            return deletedFiles.stream().mapToLong(BackupFile::getSize).sum();
        }
    }

    /**
     * Informacja o pliku, którego nie udało się usunąć.
     */
    public record FailedFile(BackupFile file, String reason) {}
}

