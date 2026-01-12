package org.example.service;

import java.io.File;
import java.util.*;

/**
 * Wynik operacji synchronizacji zawierający informacje o udanych i nieudanych lokalizacjach.
 */
public class SyncResult {

    private final List<File> successfulLocations = new ArrayList<>();
    private final Map<File, String> failedLocations = new HashMap<>();

    public void addSuccessfulLocation(File location) { successfulLocations.add(location); }
    public void addFailedLocation(File location, String error) { failedLocations.put(location, error); }

    public List<File> getSuccessfulLocations() { return new ArrayList<>(successfulLocations); }
    public Map<File, String> getFailedLocations() { return new HashMap<>(failedLocations); }

    public int getSuccessCount() { return successfulLocations.size(); }
    public int getFailureCount() { return failedLocations.size(); }

    public boolean hasFailures() { return !failedLocations.isEmpty(); }
}
