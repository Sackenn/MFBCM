package org.example.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wynik operacji synchronizacji zawierający informacje o udanych i nieudanych lokalizacjach.
 */
public class SyncResult {

    private final List<File> successfulLocations = new ArrayList<>();
    private final Map<File, String> failedLocations = new HashMap<>();

    public void addSuccessfulLocation(File location) { successfulLocations.add(location); }
    public void addFailedLocation(File location, String error) { failedLocations.put(location, error); }

    public List<File> getSuccessfulLocations() { return List.copyOf(successfulLocations); }
    public Map<File, String> getFailedLocations() { return Map.copyOf(failedLocations); }

    public int getSuccessCount() { return successfulLocations.size(); }
    public int getFailureCount() { return failedLocations.size(); }

    public boolean hasFailures() { return !failedLocations.isEmpty(); }
}

