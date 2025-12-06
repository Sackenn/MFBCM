package org.example.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of a sync operation containing information about successful and failed locations.
 */
public class SyncResult {
    private final List<File> successfulLocations;
    private final Map<File, String> failedLocations;

    public SyncResult() {
        this.successfulLocations = new ArrayList<>();
        this.failedLocations = new HashMap<>();
    }

    public void addSuccessfulLocation(File location) {
        successfulLocations.add(location);
    }

    public void addFailedLocation(File location, String error) {
        failedLocations.put(location, error);
    }

    public List<File> getSuccessfulLocations() {
        return new ArrayList<>(successfulLocations);
    }

    public Map<File, String> getFailedLocations() {
        return new HashMap<>(failedLocations);
    }

    public int getSuccessCount() {
        return successfulLocations.size();
    }

    public int getFailureCount() {
        return failedLocations.size();
    }

    public boolean hasFailures() {
        return !failedLocations.isEmpty();
    }

    public boolean isFullSuccess() {
        return failedLocations.isEmpty() && !successfulLocations.isEmpty();
    }
}

