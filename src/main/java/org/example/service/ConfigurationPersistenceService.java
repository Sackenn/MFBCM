package org.example.service;

import org.example.model.BackupConfiguration;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Service for persisting and loading application configuration.
 */
public class ConfigurationPersistenceService {

    private static final String APP_DATA_FOLDER = "MultimediaFileBackupManager";
    private static final String CONFIG_FILE_NAME = "config.properties";
    @SuppressWarnings("RegExpEmptyAlternationBranch") // Pipe character is used as literal separator, not regex
    private static final String SOURCE_DIR_SEPARATOR = "|";
    private static final String CONFIG_VERSION = "1.0";

    private final File configFile;

    public ConfigurationPersistenceService() {
        this.configFile = getConfigFile();

        // Ensure config directory exists (if needed for subdirectories)
        File configDir = configFile.getParentFile();
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                System.err.println("Failed to create config directory: " + configDir.getAbsolutePath());
            }
        }

        // Migrate from old APPDATA location if needed
        migrateFromOldLocation();
    }

    /**
     * Saves the current configuration to persistent storage.
     */
    public void saveConfiguration(BackupConfiguration config) {
        Properties properties = new Properties();

        try {
            // Save master backup location
            if (config.getMasterBackupLocation() != null) {
                properties.setProperty("masterBackupLocation", config.getMasterBackupLocation().getAbsolutePath());
            }

            // Save source directories (as pipe-separated list)
            if (!config.getSourceDirectories().isEmpty()) {
                String sourcePaths = config.getSourceDirectories().stream()
                    .map(File::getAbsolutePath)
                    .reduce((a, b) -> a + SOURCE_DIR_SEPARATOR + b)
                    .orElse("");
                properties.setProperty("sourceDirectories", sourcePaths);
            }

            // Save options
            properties.setProperty("includeSubdirectories", String.valueOf(config.isIncludeSubdirectories()));
            properties.setProperty("createDateFolders", String.valueOf(config.isCreateDateFolders()));
            properties.setProperty("hashingThreadCount", String.valueOf(config.getHashingThreadCount()));

            // Add metadata
            properties.setProperty("version", CONFIG_VERSION);
            properties.setProperty("lastSaved", String.valueOf(System.currentTimeMillis()));

            // Write to file
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                properties.store(out, "Multimedia File Backup Manager Configuration");
            }

        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    /**
     * Loads configuration from persistent storage.
     */
    public BackupConfiguration loadConfiguration() {
        BackupConfiguration config = new BackupConfiguration();

        if (!configFile.exists()) {
            return config; // Return default configuration
        }

        Properties properties = new Properties();

        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);

            // Load master backup location
            String masterPath = properties.getProperty("masterBackupLocation");
            if (masterPath != null && !masterPath.isEmpty()) {
                File masterLocation = new File(masterPath);
                if (masterLocation.exists() && masterLocation.isDirectory()) {
                    config.setMasterBackupLocation(masterLocation);
                }
            }

            // Load source directories (from pipe-separated list)
            String sourceDirectories = properties.getProperty("sourceDirectories");
            if (sourceDirectories != null && !sourceDirectories.isEmpty()) {
                String[] dirPaths = sourceDirectories.split(java.util.regex.Pattern.quote(SOURCE_DIR_SEPARATOR));
                for (String dirPath : dirPaths) {
                    File sourceDir = new File(dirPath.trim());
                    if (sourceDir.exists() && sourceDir.isDirectory()) {
                        config.addSourceDirectory(sourceDir);
                    }
                }
            }

            // Load options
            String includeSubdirectories = properties.getProperty("includeSubdirectories");
            if (includeSubdirectories != null) {
                config.setIncludeSubdirectories(Boolean.parseBoolean(includeSubdirectories));
            }


            String createDateFolders = properties.getProperty("createDateFolders");
            if (createDateFolders != null) {
                config.setCreateDateFolders(Boolean.parseBoolean(createDateFolders));
            }

            String hashingThreadCount = properties.getProperty("hashingThreadCount");
            if (hashingThreadCount != null) {
                try {
                    config.setHashingThreadCount(Integer.parseInt(hashingThreadCount));
                } catch (NumberFormatException e) {
                    // Use default value if parsing fails
                }
            }

        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
        }

        return config;
    }

    /**
     * Checks if a saved configuration exists.
     */
    public boolean hasConfiguration() {
        return configFile.exists();
    }


    /**
     * Gets the configuration file path.
     */
    public String getConfigurationPath() {
        return configFile.getAbsolutePath();
    }

    private File getConfigFile() {
        // Get the directory where the JAR file is located
        try {
            // Get the location of the current class file (JAR or class directory)
            String jarPath = ConfigurationPersistenceService.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();

            File jarFile = new File(jarPath);
            File jarDirectory;

            if (jarFile.isFile() && jarPath.endsWith(".jar")) {
                // Running from JAR file - use JAR's parent directory
                jarDirectory = jarFile.getParentFile();
            } else {
                // Running from IDE or build directory - use current working directory
                jarDirectory = new File(System.getProperty("user.dir"));
            }

            return new File(jarDirectory, CONFIG_FILE_NAME);

        } catch (Exception e) {
            // Fallback to current working directory if we can't determine JAR location
            System.err.println("Could not determine JAR location, using current directory: " + e.getMessage());
            return new File(System.getProperty("user.dir"), CONFIG_FILE_NAME);
        }
    }

    /**
     * Migrates configuration from the old APPDATA location to the new JAR directory location.
     */
    private void migrateFromOldLocation() {
        // Only migrate if new config file doesn't exist yet
        if (configFile.exists()) {
            return;
        }

        File oldConfigFile = getOldConfigFile();
        if (oldConfigFile != null && oldConfigFile.exists()) {
            try {
                // Copy the old config file to the new location
                java.nio.file.Files.copy(oldConfigFile.toPath(), configFile.toPath());
                System.out.println("Migrated configuration from: " + oldConfigFile.getAbsolutePath() +
                                 " to: " + configFile.getAbsolutePath());

                // Optionally delete the old config file and directory
                try {
                    if (!oldConfigFile.delete()) {
                        System.out.println("Note: Could not delete old config file");
                    }
                    File oldConfigDir = oldConfigFile.getParentFile();
                    String[] dirContents = oldConfigDir.list();
                    if (oldConfigDir.isDirectory() && dirContents != null && dirContents.length == 0) {
                        if (!oldConfigDir.delete()) {
                            System.out.println("Note: Could not delete old config directory");
                        }
                    }
                } catch (Exception e) {
                    // Don't fail migration if we can't clean up old files
                    System.out.println("Note: Could not clean up old config file: " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Failed to migrate configuration: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the old config file location (APPDATA).
     */
    private File getOldConfigFile() {
        try {
            String userHome = System.getProperty("user.home");
            String os = System.getProperty("os.name").toLowerCase();

            Path configPath;
            if (os.contains("win")) {
                // Windows: %APPDATA%
                String appData = System.getenv("APPDATA");
                if (appData != null) {
                    configPath = Paths.get(appData, APP_DATA_FOLDER, CONFIG_FILE_NAME);
                } else {
                    configPath = Paths.get(userHome, "AppData", "Roaming", APP_DATA_FOLDER, CONFIG_FILE_NAME);
                }
            } else if (os.contains("mac")) {
                // macOS: ~/Library/Application Support
                configPath = Paths.get(userHome, "Library", "Application Support", APP_DATA_FOLDER, CONFIG_FILE_NAME);
            } else {
                // Linux/Unix: ~/.config
                configPath = Paths.get(userHome, ".config", APP_DATA_FOLDER, CONFIG_FILE_NAME);
            }

            return configPath.toFile();
        } catch (Exception e) {
            return null;
        }
    }
}
