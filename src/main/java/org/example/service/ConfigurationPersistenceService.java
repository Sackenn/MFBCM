package org.example.service;

import org.example.model.BackupConfiguration;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Serwis trwałego przechowywania konfiguracji aplikacji.
 */
public class ConfigurationPersistenceService {

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String SOURCE_DIR_SEPARATOR = "|";

    private final File configFile;

    public ConfigurationPersistenceService() {
        this.configFile = getConfigFile();
        ensureConfigDirectoryExists();
    }

    private void ensureConfigDirectoryExists() {
        File configDir = configFile.getParentFile();
        if (!configDir.exists() && !configDir.mkdirs()) {
            System.err.println("Failed to create config directory: " + configDir.getAbsolutePath());
        }
    }

    // ====== ZAPISYWANIE ======

    public void saveConfiguration(BackupConfiguration config) {
        Properties properties = new Properties();

        if (config.getMasterBackupLocation() != null) {
            properties.setProperty("masterBackupLocation", config.getMasterBackupLocation().getAbsolutePath());
        }

        if (!config.getSourceDirectories().isEmpty()) {
            String sourcePaths = config.getSourceDirectories().stream()
                    .map(File::getAbsolutePath)
                    .reduce((a, b) -> a + SOURCE_DIR_SEPARATOR + b)
                    .orElse("");
            properties.setProperty("sourceDirectories", sourcePaths);
        }

        if (!config.getSyncLocations().isEmpty()) {
            String syncPaths = config.getSyncLocations().stream()
                    .map(File::getAbsolutePath)
                    .reduce((a, b) -> a + SOURCE_DIR_SEPARATOR + b)
                    .orElse("");
            properties.setProperty("syncLocations", syncPaths);
        }

        properties.setProperty("includeSubdirectories", String.valueOf(config.isIncludeSubdirectories()));
        properties.setProperty("createDateFolders", String.valueOf(config.isCreateDateFolders()));
        properties.setProperty("skipHashing", String.valueOf(config.isSkipHashing()));
        properties.setProperty("hashingThreadCount", String.valueOf(config.getHashingThreadCount()));
        properties.setProperty("lastSaved", String.valueOf(System.currentTimeMillis()));

        try (FileOutputStream out = new FileOutputStream(configFile)) {
            properties.store(out, "Multimedia File Backup Manager Configuration");
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    // ====== WCZYTYWANIE ======

    public BackupConfiguration loadConfiguration() {
        BackupConfiguration config = new BackupConfiguration();

        if (!configFile.exists()) return config;

        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
            loadMasterLocation(properties, config);
            loadSourceDirectories(properties, config);
            loadSyncLocations(properties, config);
            loadOptions(properties, config);
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }

        return config;
    }

    private void loadMasterLocation(Properties properties, BackupConfiguration config) {
        String masterPath = properties.getProperty("masterBackupLocation");
        if (masterPath != null && !masterPath.isEmpty()) {
            File masterLocation = new File(masterPath);
            if (masterLocation.exists() && masterLocation.isDirectory()) {
                config.setMasterBackupLocation(masterLocation);
            }
        }
    }

    private void loadSourceDirectories(Properties properties, BackupConfiguration config) {
        String sourceDirectories = properties.getProperty("sourceDirectories");
        if (sourceDirectories != null && !sourceDirectories.isEmpty()) {
            String[] dirPaths = sourceDirectories.split(Pattern.quote(SOURCE_DIR_SEPARATOR));
            for (String dirPath : dirPaths) {
                File sourceDir = new File(dirPath.trim());
                if (sourceDir.exists() && sourceDir.isDirectory()) {
                    config.addSourceDirectory(sourceDir);
                }
            }
        }
    }

    private void loadSyncLocations(Properties properties, BackupConfiguration config) {
        String syncLocations = properties.getProperty("syncLocations");
        if (syncLocations != null && !syncLocations.isEmpty()) {
            String[] locPaths = syncLocations.split(Pattern.quote(SOURCE_DIR_SEPARATOR));
            for (String locPath : locPaths) {
                File syncLoc = new File(locPath.trim());
                if (syncLoc.exists() && syncLoc.isDirectory()) {
                    config.addSyncLocation(syncLoc);
                }
            }
        }
    }

    private void loadOptions(Properties properties, BackupConfiguration config) {
        String includeSubdirectories = properties.getProperty("includeSubdirectories");
        if (includeSubdirectories != null) {
            config.setIncludeSubdirectories(Boolean.parseBoolean(includeSubdirectories));
        }

        String createDateFolders = properties.getProperty("createDateFolders");
        if (createDateFolders != null) {
            config.setCreateDateFolders(Boolean.parseBoolean(createDateFolders));
        }

        String skipHashing = properties.getProperty("skipHashing");
        if (skipHashing != null) {
            config.setSkipHashing(Boolean.parseBoolean(skipHashing));
        }

        String hashingThreadCount = properties.getProperty("hashingThreadCount");
        if (hashingThreadCount != null) {
            try {
                config.setHashingThreadCount(Integer.parseInt(hashingThreadCount));
            } catch (NumberFormatException e) {
                // Użyj domyślnej wartości
            }
        }
    }

    // ====== POMOCNICZE ======

    public boolean hasConfiguration() {
        return configFile.exists();
    }

    public String getConfigurationPath() {
        return configFile.getAbsolutePath();
    }

    private File getConfigFile() {
        try {
            String jarPath = ConfigurationPersistenceService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();

            File jarFile = new File(jarPath);
            File jarDirectory = (jarFile.isFile() && jarPath.endsWith(".jar"))
                ? jarFile.getParentFile()
                : new File(System.getProperty("user.dir"));

            return new File(jarDirectory, CONFIG_FILE_NAME);
        } catch (Exception e) {
            System.err.println("Could not determine JAR location, using current directory: " + e.getMessage());
            return new File(System.getProperty("user.dir"), CONFIG_FILE_NAME);
        }
    }
}