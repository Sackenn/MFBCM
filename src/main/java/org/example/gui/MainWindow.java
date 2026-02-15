package org.example.gui;

import org.example.model.BackupConfiguration;
import org.example.model.BackupFile;
import org.example.model.DuplicateAnalysisResult;
import org.example.model.SyncResult;
import org.example.service.*;
import org.example.util.LanguageManager;
import org.example.util.MultiThreadedHashCalculator;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.example.gui.UIConstants.*;
import static org.example.util.LanguageManager.get;

/**
 * Glowne okno aplikacji do zarzadzania kopiami zapasowymi plikow multimedialnych.
 */
public class MainWindow extends JFrame implements FileScanner.ScanProgressCallback,
                                                 BackupService.BackupProgressCallback,
                                                 DuplicateDetectionService.DuplicateDetectionCallback,
                                                 SyncService.SyncProgressCallback,
                                                 FileDeleteService.DeleteProgressCallback,
                                                 LanguageManager.LanguageChangeListener {

    private final BackupConfiguration configuration;
    private HashStorageService hashStorageService;
    private final ConfigurationPersistenceService configPersistenceService;

    // Komponenty UI - Konfiguracja
    private JLabel masterLocationLabel, masterLocationTitle;
    private JButton browseMasterButton;
    private JList<File> sourceDirectoriesList;
    private DefaultListModel<File> sourceListModel;
    private JButton addSourceButton, removeSourceButton;
    private JLabel sourceDirectoriesTitle;
    private JList<File> syncLocationsList;
    private DefaultListModel<File> syncListModel;
    private JButton addSyncButton, removeSyncButton;
    private JLabel syncLocationsTitle;

    // Komponenty UI - Akcje
    private JButton scanButton, backupButton, viewDuplicatesButton, rescanMasterButton, syncButton, deleteSelectedButton;

    // Komponenty UI - Panel plikow i postep
    private FileListPanel fileListPanel;
    private JProgressBar scanProgressBar, backupProgressBar, syncProgressBar;
    private JLabel statusLabel;

    // Komponenty UI - Opcje
    private JCheckBox includeSubdirectoriesCheckBox, createDateFoldersCheckBox;
    private JCheckBox enableDuplicateDetectionCheckBox, skipHashingCheckBox;
    private JSpinner threadCountSpinner;
    private JLabel hashThreadsLabel;
    private JComboBox<String> languageComboBox;

    // Komponenty UI - Panele z tytulami
    private JPanel configPanel, progressPanel;

    // Serwisy i stan
    private FileScanner currentScanner;
    private BackupService currentBackupService;
    private DuplicateDetectionService currentDuplicateService;
    private SyncService currentSyncService;
    private FileDeleteService currentDeleteService;
    private DuplicateAnalysisResult lastDuplicateResult;
    private String lastTimingInfo;

    public MainWindow() {
        this.configPersistenceService = new ConfigurationPersistenceService();
        this.configuration = configPersistenceService.loadConfiguration();
        LanguageManager.addLanguageChangeListener(this);
        initializeUI();
        setupEventHandlers();
        loadSavedConfiguration();
        initializeHashStorage();
    }

    private void initializeUI() {
        setTitle(get("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Dekoracja okna
        getRootPane().putClientProperty("JRootPane.titleBarBackground", BG_PRIMARY);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.WHITE);

        // Główny kontener
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainContainer.add(createConfigurationPanel(), BorderLayout.NORTH);
        mainContainer.add(createCenterPanel(), BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        updateButtonStates();

        // Ustawienia okna
        setMinimumSize(new Dimension(1000, 750));
        setPreferredSize(new Dimension(1200, 1000));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createConfigurationPanel() {
        configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(createTitledBorder(get("config.title")));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = CELL_INSETS;

        // Lokalizacja glownej kopii zapasowej
        addMasterLocationRow(configPanel, gbc);

        // Katalogi zrodlowe
        addSourceDirectoriesRow(configPanel, gbc);

        // Lokalizacje synchronizacji
        addSyncLocationsRow(configPanel, gbc);

        // Opcje
        addOptionsRow(configPanel, gbc);

        // Przyciski akcji
        addActionButtonsRow(configPanel, gbc);

        return configPanel;
    }

    private void addMasterLocationRow(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        masterLocationTitle = createLabel(get("config.masterLocation"));
        panel.add(masterLocationTitle, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        masterLocationLabel = createLabel(get("config.noLocationSelected"), FONT_REGULAR, TEXT_SECONDARY);
        masterLocationLabel.setOpaque(true);
        masterLocationLabel.setBackground(BG_INPUT);
        masterLocationLabel.setBorder(createInputBorder());
        panel.add(masterLocationLabel, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        browseMasterButton = createButton(get("button.browse"), BUTTON_SMALL);
        panel.add(browseMasterButton, gbc);
    }

    private void addSourceDirectoriesRow(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        sourceDirectoriesTitle = createLabel(get("config.sourceDirectories"));
        panel.add(sourceDirectoriesTitle, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        sourceListModel = new DefaultListModel<>();
        sourceDirectoriesList = new JList<>(sourceListModel);
        styleList(sourceDirectoriesList);
        panel.add(createScrollPane(sourceDirectoriesList), gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        buttonPanel.setOpaque(false);
        addSourceButton = createButton(get("button.add"), BUTTON_SMALL);
        removeSourceButton = createButton(get("button.remove"), BUTTON_SMALL);
        buttonPanel.add(addSourceButton);
        buttonPanel.add(removeSourceButton);
        panel.add(buttonPanel, gbc);
    }

    private void addSyncLocationsRow(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 1.0;
        syncLocationsTitle = createLabel(get("config.syncLocations"));
        syncLocationsTitle.setToolTipText(get("config.syncLocationsTooltip"));
        panel.add(syncLocationsTitle, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        syncListModel = new DefaultListModel<>();
        syncLocationsList = new JList<>(syncListModel);
        styleList(syncLocationsList);
        panel.add(createScrollPane(syncLocationsList), gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        buttonPanel.setOpaque(false);
        addSyncButton = createButton(get("button.add"), BUTTON_SMALL);
        removeSyncButton = createButton(get("button.remove"), BUTTON_SMALL);
        buttonPanel.add(addSyncButton);
        buttonPanel.add(removeSyncButton);
        panel.add(buttonPanel, gbc);
    }

    private void addOptionsRow(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        // Uzyj WrapLayout wycentrowany dla lepszego skalowania przy zmianie jezyka
        JPanel optionsPanel = new JPanel(new WrapLayout(FlowLayout.CENTER, 15, 5));
        optionsPanel.setOpaque(false);

        includeSubdirectoriesCheckBox = createCheckBox(get("option.includeSubdirectories"), true);
        createDateFoldersCheckBox = createCheckBox(get("option.createDateFolders"), false);
        enableDuplicateDetectionCheckBox = createCheckBox(get("option.detectDuplicates"), true);
        skipHashingCheckBox = createCheckBox(get("option.skipHashing"), false);
        skipHashingCheckBox.setToolTipText(get("option.skipHashingTooltip"));

        optionsPanel.add(includeSubdirectoriesCheckBox);
        optionsPanel.add(createDateFoldersCheckBox);
        optionsPanel.add(enableDuplicateDetectionCheckBox);
        optionsPanel.add(skipHashingCheckBox);

        // Grupuj hash threads label i spinner razem
        JPanel hashThreadsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        hashThreadsPanel.setOpaque(false);
        hashThreadsLabel = createLabel(get("option.hashThreads"));
        hashThreadsPanel.add(hashThreadsLabel);
        threadCountSpinner = new JSpinner(new SpinnerNumberModel(
            Runtime.getRuntime().availableProcessors(), 1,
            Runtime.getRuntime().availableProcessors() * 2, 1));
        threadCountSpinner.setFont(FONT_REGULAR);
        threadCountSpinner.setPreferredSize(SPINNER_SIZE);
        hashThreadsPanel.add(threadCountSpinner);
        optionsPanel.add(hashThreadsPanel);

        // Selektor jezyka - grupuj label i combo razem
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        langPanel.setOpaque(false);
        JLabel langLabel = createLabel(get("app.language") + ":");
        langPanel.add(langLabel);
        languageComboBox = new JComboBox<>();
        for (String code : LanguageManager.getAvailableLanguageCodes()) {
            languageComboBox.addItem(LanguageManager.getLanguageDisplayName(code));
        }
        languageComboBox.setSelectedIndex(LanguageManager.getAvailableLanguageCodes()
            .indexOf(LanguageManager.getCurrentLanguageCode()));
        languageComboBox.setFont(FONT_REGULAR);
        languageComboBox.addActionListener(_ -> {
            int index = languageComboBox.getSelectedIndex();
            String code = LanguageManager.getAvailableLanguageCodes().get(index);
            LanguageManager.setLanguage(code);
        });
        langPanel.add(languageComboBox);
        optionsPanel.add(langPanel);

        panel.add(optionsPanel, gbc);
    }

    private void addActionButtonsRow(JPanel panel, GridBagConstraints gbc) {
        gbc.gridy = 4;
        gbc.insets = new Insets(6, 8, 4, 8);

        // Uzyj WrapLayout dla lepszego skalowania przy zmianie jezyka
        JPanel actionPanel = new JPanel(new WrapLayout(FlowLayout.CENTER, 12, 5));
        actionPanel.setOpaque(false);

        scanButton = createButton(get("button.scanFiles"));
        backupButton = createButton(get("button.startBackup"));
        viewDuplicatesButton = createButton(get("button.viewDuplicates"));
        viewDuplicatesButton.setEnabled(false);
        rescanMasterButton = createButton(get("button.rescanMaster"));
        rescanMasterButton.setEnabled(false);
        syncButton = createButton(get("button.syncLocations"));
        syncButton.setEnabled(false);
        deleteSelectedButton = createButton(get("button.deleteSelected"));
        deleteSelectedButton.setEnabled(false);

        actionPanel.add(scanButton);
        actionPanel.add(backupButton);
        actionPanel.add(viewDuplicatesButton);
        actionPanel.add(rescanMasterButton);
        actionPanel.add(syncButton);
        actionPanel.add(deleteSelectedButton);

        panel.add(actionPanel, gbc);
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        fileListPanel = new FileListPanel();
        fileListPanel.setSelectionChangeListener(this::updateButtonStates);
        panel.add(fileListPanel, BorderLayout.CENTER);

        // Paski postepu
        progressPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        progressPanel.setBorder(createTitledBorder(get("progress.title")));

        scanProgressBar = createProgressBar(get("progress.readyToScan"));
        backupProgressBar = createProgressBar(get("progress.readyToBackup"));
        syncProgressBar = createProgressBar(get("progress.readyToSync"));

        progressPanel.add(scanProgressBar);
        progressPanel.add(backupProgressBar);
        progressPanel.add(syncProgressBar);

        panel.add(progressPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(createStatusBorder());

        statusLabel = createLabel(get("status.ready"), FONT_REGULAR, TEXT_SECONDARY);
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void setupEventHandlers() {
        // Nawigacja
        browseMasterButton.addActionListener(_ -> browseMasterLocation());
        addSourceButton.addActionListener(_ -> addSourceDirectory());
        removeSourceButton.addActionListener(_ -> removeSourceDirectory());
        addSyncButton.addActionListener(_ -> addSyncLocation());
        removeSyncButton.addActionListener(_ -> removeSyncLocation());

        // Akcje
        scanButton.addActionListener(_ -> startScan());
        backupButton.addActionListener(_ -> startBackup());
        viewDuplicatesButton.addActionListener(_ -> viewDuplicates());
        rescanMasterButton.addActionListener(_ -> rescanMasterFolder());
        syncButton.addActionListener(_ -> startSync());
        deleteSelectedButton.addActionListener(_ -> deleteSelectedFiles());

        // Opcje
        includeSubdirectoriesCheckBox.addActionListener(_ -> {
            configuration.setIncludeSubdirectories(includeSubdirectoriesCheckBox.isSelected());
            saveConfiguration();
        });

        createDateFoldersCheckBox.addActionListener(_ -> {
            configuration.setCreateDateFolders(createDateFoldersCheckBox.isSelected());
            saveConfiguration();
        });

        skipHashingCheckBox.addActionListener(_ -> {
            configuration.setSkipHashing(skipHashingCheckBox.isSelected());
            updateHashingControlsState();
            saveConfiguration();
        });

        threadCountSpinner.addChangeListener(_ -> {
            configuration.setHashingThreadCount((Integer) threadCountSpinner.getValue());
            saveConfiguration();
        });

        // Selekcja list
        sourceDirectoriesList.addListSelectionListener(_ -> updateButtonStates());
        syncLocationsList.addListSelectionListener(_ -> updateButtonStates());
    }

    // ====== OPERACJE NAWIGACJI ======

    private void browseMasterLocation() {
        File selectedDir = showDirectoryChooser("Select Master Backup Location",
            configuration.getMasterBackupLocation());

        if (selectedDir != null) {
            configuration.setMasterBackupLocation(selectedDir);
            masterLocationLabel.setText(selectedDir.getAbsolutePath());
            initializeHashStorage();
            updateButtonStates();
            saveConfiguration();
        }
    }

    private void addSourceDirectory() {
        File selectedDir = showDirectoryChooser("Select Source Directory", null);
        if (selectedDir != null) {
            configuration.addSourceDirectory(selectedDir);
            sourceListModel.addElement(selectedDir);
            updateButtonStates();
            saveConfiguration();
        }
    }

    private void removeSourceDirectory() {
        File selectedDir = sourceDirectoriesList.getSelectedValue();
        if (selectedDir != null) {
            configuration.removeSourceDirectory(selectedDir);
            sourceListModel.removeElement(selectedDir);
            updateButtonStates();
            saveConfiguration();
        }
    }

    private void addSyncLocation() {
        File selectedDir = showDirectoryChooser("Select Sync Location", null);
        if (selectedDir != null) {
            if (selectedDir.equals(configuration.getMasterBackupLocation())) {
                JOptionPane.showMessageDialog(this,
                    "Sync location cannot be the same as master backup location.",
                    "Invalid Location", JOptionPane.WARNING_MESSAGE);
                return;
            }
            configuration.addSyncLocation(selectedDir);
            syncListModel.addElement(selectedDir);
            updateButtonStates();
            saveConfiguration();
        }
    }

    private void removeSyncLocation() {
        File selectedDir = syncLocationsList.getSelectedValue();
        if (selectedDir != null) {
            configuration.removeSyncLocation(selectedDir);
            syncListModel.removeElement(selectedDir);
            updateButtonStates();
            saveConfiguration();
        }
    }

    private File showDirectoryChooser(String title, File currentDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(title);
        if (currentDir != null) {
            chooser.setCurrentDirectory(currentDir);
        }
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION
            ? chooser.getSelectedFile() : null;
    }

    // ====== OPERACJE SKANOWANIA I KOPII ZAPASOWEJ ======

    private void startScan() {
        if (isScanInProgress()) {
            cancelCurrentScan();
            return;
        }

        fileListPanel.clearFiles();
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Starting scan...");

        if (enableDuplicateDetectionCheckBox.isSelected() && hashStorageService != null) {
            currentDuplicateService = new DuplicateDetectionService(configuration, hashStorageService, this);
            currentDuplicateService.execute();
        } else {
            currentScanner = new FileScanner(configuration, this);
            currentScanner.execute();
        }

        scanButton.setText("Cancel Scan");
        backupButton.setEnabled(false);
        viewDuplicatesButton.setEnabled(false);
    }

    private void startBackup() {
        if (isBackupInProgress()) {
            if (confirmAction("Are you sure you want to cancel the backup?", "Cancel Backup")) {
                cancelBackup();
            }
            return;
        }

        List<BackupFile> selectedFiles = fileListPanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            showWarning("No files selected for backup.", "No Files Selected");
            return;
        }

        if (confirmAction("Start backup of " + selectedFiles.size() + " files?", "Confirm Backup")) {
            backupProgressBar.setValue(0);
            backupProgressBar.setString("Starting backup...");

            currentBackupService = new BackupService(fileListPanel.getAllFiles(), configuration, this);
            currentBackupService.execute();

            backupButton.setText("Cancel Backup");
            scanButton.setEnabled(false);
        }
    }

    private void startSync() {
        if (isSyncInProgress()) {
            if (confirmAction("Are you sure you want to cancel the sync?", "Cancel Sync")) {
                cancelSync();
            }
            return;
        }

        List<File> syncLocations = configuration.getSyncLocations();
        StringBuilder message = new StringBuilder("Sync master folder to the following locations?\n\n");
        syncLocations.forEach(loc -> message.append("• ").append(loc.getAbsolutePath()).append("\n"));
        message.append("\nThis will copy all files from the master folder and remove files that no longer exist.");

        if (confirmAction(message.toString(), "Confirm Sync")) {
            syncProgressBar.setValue(0);
            syncProgressBar.setString("Starting sync...");
            statusLabel.setText("Syncing master folder...");

            currentSyncService = new SyncService(configuration, this);
            currentSyncService.execute();

            syncButton.setText("Cancel Sync");
            updateButtonStates();
        }
    }

    private void deleteSelectedFiles() {
        if (isDeleteInProgress()) {
            if (confirmAction("Are you sure you want to cancel the delete operation?", "Cancel Delete")) {
                cancelDelete();
            }
            return;
        }

        List<BackupFile> selectedFiles = fileListPanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            showWarning("No files selected for deletion.", "No Selection");
            return;
        }

        long totalSize = selectedFiles.stream().mapToLong(BackupFile::getSize).sum();
        String sizeText = org.example.util.FileUtilities.formatFileSize(totalSize);

        String message = String.format("""
            Are you sure you want to permanently delete %d selected files?
            
            Total size: %s
            
            WARNING: This action cannot be undone!""",
            selectedFiles.size(), sizeText);

        if (!confirmAction(message, "Confirm Delete")) {
            return;
        }

        scanProgressBar.setValue(0);
        scanProgressBar.setString("Deleting files...");
        statusLabel.setText("Deleting " + selectedFiles.size() + " files...");

        currentDeleteService = new FileDeleteService(selectedFiles, this);
        currentDeleteService.execute();

        deleteSelectedButton.setText("Cancel Delete");
        updateButtonStates();
    }

    // ====== POMOCNICZE METODY STANU ======

    private boolean isScanInProgress() {
        return (currentScanner != null && !currentScanner.isDone()) ||
               (currentDuplicateService != null && !currentDuplicateService.isDone());
    }

    private boolean isBackupInProgress() {
        return currentBackupService != null && !currentBackupService.isDone();
    }

    private boolean isSyncInProgress() {
        return currentSyncService != null && !currentSyncService.isDone();
    }

    private boolean isDeleteInProgress() {
        return currentDeleteService != null && !currentDeleteService.isDone();
    }

    private void cancelCurrentScan() {
        if (currentScanner != null && !currentScanner.isDone()) {
            currentScanner.cancel(true);
            currentScanner = null;
        }
        if (currentDuplicateService != null && !currentDuplicateService.isDone()) {
            currentDuplicateService.cancel(true);
            currentDuplicateService = null;
        }
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Scan cancelled");
        statusLabel.setText("Scan cancelled by user");
        updateButtonStates();
    }

    private void cancelBackup() {
        currentBackupService.cancel(true);
        currentBackupService = null;
        backupProgressBar.setValue(0);
        backupProgressBar.setString("Backup cancelled");
        statusLabel.setText("Backup cancelled by user");
        updateButtonStates();
    }

    private void cancelSync() {
        currentSyncService.cancel(true);
        currentSyncService = null;
        syncProgressBar.setValue(0);
        syncProgressBar.setString("Sync cancelled");
        statusLabel.setText("Sync cancelled by user");
        updateButtonStates();
    }

    private void cancelDelete() {
        currentDeleteService.cancel(true);
        currentDeleteService = null;
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Delete cancelled");
        statusLabel.setText("Delete cancelled by user");
        deleteSelectedButton.setText("Delete Selected");
        updateButtonStates();
    }

    // ====== METODY POMOCNICZE UI ======

    private boolean confirmAction(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void updateButtonStates() {
        removeSourceButton.setEnabled(sourceDirectoriesList.getSelectedValue() != null);
        removeSyncButton.setEnabled(syncLocationsList.getSelectedValue() != null);

        boolean canScan = configuration.getMasterBackupLocation() != null &&
                         !configuration.getSourceDirectories().isEmpty();
        boolean scanInProgress = isScanInProgress();

        scanButton.setEnabled(canScan);
        scanButton.setText(scanInProgress ? "Cancel Scan" : "Scan for Files");

        boolean hasSelectedFiles = !fileListPanel.getSelectedFiles().isEmpty();
        boolean backupInProgress = isBackupInProgress();

        backupButton.setEnabled(hasSelectedFiles && !scanInProgress && canScan);
        backupButton.setText(backupInProgress ? "Cancel Backup" : "Start Backup");

        if (!viewDuplicatesButton.isEnabled() && lastDuplicateResult != null &&
            lastDuplicateResult.getTotalDuplicateCount() > 0) {
            viewDuplicatesButton.setEnabled(true);
        }

        boolean canRescanMaster = configuration.getMasterBackupLocation() != null &&
                                 !scanInProgress && !backupInProgress;
        rescanMasterButton.setEnabled(canRescanMaster);

        boolean syncInProgress = isSyncInProgress();
        boolean canSync = configuration.getMasterBackupLocation() != null &&
                         !configuration.getSyncLocations().isEmpty() &&
                         !scanInProgress && !backupInProgress;
        syncButton.setEnabled(canSync || syncInProgress);
        syncButton.setText(syncInProgress ? "Cancel Sync" : "Sync Locations");

        deleteSelectedButton.setEnabled(hasSelectedFiles && !scanInProgress && !backupInProgress && !syncInProgress);
    }

    private void updateHashingControlsState() {
        threadCountSpinner.setEnabled(!skipHashingCheckBox.isSelected());
    }

    // ====== IMPLEMENTACJA CALLBACKÓW ======

    @Override
    public void updateProgress(int current, int total, String currentFile) {
        SwingUtilities.invokeLater(() -> {
            if (total > 0) {
                scanProgressBar.setValue((current * 100) / total);
            }

            boolean isDuplicateDetection = currentDuplicateService != null && !currentDuplicateService.isDone();

            if (current >= total && currentFile.startsWith("Completed in ")) {
                lastTimingInfo = currentFile;
                String prefix = isDuplicateDetection ? "Duplicate detection " : "Scan ";
                scanProgressBar.setString(prefix + currentFile);
            } else if (isDuplicateDetection) {
                scanProgressBar.setString("Detecting duplicates: " + currentFile);
                statusLabel.setText("Analyzed " + current + " of " + total + " files for duplicates");
            } else {
                scanProgressBar.setString("Scanning: " + currentFile);
                statusLabel.setText("Scanned " + current + " of " + total + " files");
            }
        });
    }

    @Override
    public void scanCompleted(List<BackupFile> files) {
        SwingUtilities.invokeLater(() -> {
            fileListPanel.setFiles(files);
            scanProgressBar.setValue(100);
            String message = lastTimingInfo != null
                ? "Scan " + lastTimingInfo + " - " + files.size() + " files found"
                : "Scan completed - " + files.size() + " files found";
            scanProgressBar.setString(message);
            lastTimingInfo = null;
            statusLabel.setText("Found " + files.size() + " multimedia files");
            updateButtonStates();
        });
    }

    @Override
    public void scanFailed(String error) {
        SwingUtilities.invokeLater(() -> {
            scanProgressBar.setString("Scan failed: " + error);
            statusLabel.setText("Scan failed");
            updateButtonStates();
            JOptionPane.showMessageDialog(this, "Scan failed: " + error,
                    "Scan Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void updateProgress(int current, int total, String currentFile, long bytesProcessed, long totalBytes) {
        SwingUtilities.invokeLater(() -> {
            if (total > 0) {
                int percentage = (current * 100) / total;
                String bytesText = totalBytes > 0
                    ? String.format(" (%.1f%% of data)", (bytesProcessed * 100.0) / totalBytes) : "";

                if (isBackupInProgress()) {
                    backupProgressBar.setValue(percentage);
                    backupProgressBar.setString("Backing up: " + currentFile);
                    statusLabel.setText("Backed up " + current + " of " + total + " files" + bytesText);
                } else if (isSyncInProgress()) {
                    syncProgressBar.setValue(percentage);
                    syncProgressBar.setString("Syncing: " + currentFile);
                    statusLabel.setText("Synced " + current + " of " + total + " files" + bytesText);
                }
            }
        });
    }

    @Override
    public void backupCompleted(int successCount, int errorCount) {
        SwingUtilities.invokeLater(() -> {
            backupProgressBar.setValue(100);
            backupProgressBar.setString("Backup completed - " + successCount + " files copied");
            statusLabel.setText("Backup completed: " + successCount + " successful, " + errorCount + " errors");
            updateButtonStates();

            String message = String.format("Backup completed!\n\nSuccessfully copied: %d files\nErrors: %d files",
                    successCount, errorCount);
            JOptionPane.showMessageDialog(this, message, "Backup Complete",
                    errorCount == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

            if (successCount > 0) {
                statusLabel.setText("Refreshing master folder...");
                performAutomaticMasterRescan();
            }
        });
    }

    @Override
    public void backupFailed(String error, int filesBackedUpBeforeCancellation) {
        SwingUtilities.invokeLater(() -> {
            boolean wasCancelled = error != null && error.toLowerCase().contains("cancel");

            if (wasCancelled) {
                backupProgressBar.setString("Backup cancelled");
                statusLabel.setText(filesBackedUpBeforeCancellation > 0
                    ? "Backup cancelled - " + filesBackedUpBeforeCancellation + " files were copied before cancellation"
                    : "Backup cancelled by user");

                if (filesBackedUpBeforeCancellation > 0) {
                    statusLabel.setText("Backup cancelled - Refreshing master folder...");
                    performAutomaticMasterRescan();
                }
            } else {
                backupProgressBar.setString("Backup failed: " + error);
                statusLabel.setText("Backup failed");
                JOptionPane.showMessageDialog(this, "Backup failed: " + error,
                        "Backup Error", JOptionPane.ERROR_MESSAGE);
            }
            updateButtonStates();
        });
    }

    @Override
    public void fileCompleted(BackupFile file, boolean success, String error) {
        SwingUtilities.invokeLater(() -> fileListPanel.updateFileStatus(file));
    }

    @Override
    public void detectionCompleted(DuplicateAnalysisResult result) {
        SwingUtilities.invokeLater(() -> {
            this.lastDuplicateResult = result;
            fileListPanel.setFiles(result.getSourceFiles());
            scanProgressBar.setValue(100);

            String message = result.getProcessingTimeMs() > 0
                ? "Duplicate detection completed in " + result.getFormattedDuration() +
                  " (" + String.format("%.1f", result.getThroughputMbPerSec()) + " MB/s) - " +
                  result.getTotalSourceFiles() + " files found"
                : "Duplicate detection completed - " + result.getTotalSourceFiles() + " files found";

            scanProgressBar.setString(message);
            statusLabel.setText("Found " + result.getNewFileCount() + " new files, " +
                              result.getTotalDuplicateCount() + " duplicates");
            viewDuplicatesButton.setEnabled(result.getTotalDuplicateCount() > 0);
            updateButtonStates();
        });
    }

    @Override
    public void detectionFailed(String error) {
        SwingUtilities.invokeLater(() -> {
            scanProgressBar.setString("Duplicate detection failed: " + error);
            statusLabel.setText("Duplicate detection failed");
            updateButtonStates();
            JOptionPane.showMessageDialog(this, "Duplicate detection failed: " + error,
                    "Detection Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void syncCompleted(SyncResult result) {
        SwingUtilities.invokeLater(() -> {
            syncProgressBar.setValue(100);
            syncProgressBar.setString("Sync completed - " + result.getSuccessCount() + " locations synced");
            statusLabel.setText("Sync completed: " + result.getSuccessCount() + " successful, " +
                              result.getFailureCount() + " failed");
            updateButtonStates();

            StringBuilder message = new StringBuilder("Sync completed!\n\n");
            if (!result.getSuccessfulLocations().isEmpty()) {
                message.append("Successfully synced to:\n");
                result.getSuccessfulLocations().forEach(loc ->
                    message.append("• ").append(loc.getAbsolutePath()).append("\n"));
            }
            if (!result.getFailedLocations().isEmpty()) {
                message.append("\nFailed to sync to:\n");
                for (Map.Entry<File, String> entry : result.getFailedLocations().entrySet()) {
                    message.append("• ").append(entry.getKey().getAbsolutePath())
                           .append("\n  Error: ").append(entry.getValue()).append("\n");
                }
            }

            JOptionPane.showMessageDialog(this, message.toString(), "Sync Complete",
                    result.hasFailures() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        });
    }

    @Override
    public void syncFailed(String error) {
        SwingUtilities.invokeLater(() -> {
            syncProgressBar.setString("Sync failed: " + error);
            statusLabel.setText("Sync failed");
            updateButtonStates();
            JOptionPane.showMessageDialog(this, "Sync failed: " + error,
                    "Sync Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    // ====== IMPLEMENTACJA CALLBACKÓW USUWANIA ======

    @Override
    public void deleteCompleted(FileDeleteService.DeleteResult result) {
        SwingUtilities.invokeLater(() -> {
            scanProgressBar.setValue(100);
            deleteSelectedButton.setText("Delete Selected");

            // Odśwież listę plików - usuń usunięte pliki
            List<BackupFile> remainingFiles = fileListPanel.getAllFiles().stream()
                .filter(f -> f.getSourceFile().exists())
                .toList();
            fileListPanel.setFiles(new java.util.ArrayList<>(remainingFiles));
            fileListPanel.updateSummary();

            String sizeText = org.example.util.FileUtilities.formatFileSize(result.getTotalDeletedSize());
            scanProgressBar.setString("Deleted " + result.getDeletedCount() + " files (" + sizeText + ")");
            statusLabel.setText("Deleted " + result.getDeletedCount() + " files" +
                (result.hasFailures() ? " (" + result.getFailedCount() + " failed)" : ""));

            // Pokaż wynik operacji
            if (result.hasFailures()) {
                StringBuilder message = new StringBuilder();
                message.append(String.format("Deleted: %d files\nFailed: %d files\n\n",
                    result.getDeletedCount(), result.getFailedCount()));
                message.append("Failed files:\n");
                result.getFailedFiles().stream().limit(10).forEach(f ->
                    message.append("• ").append(f.file().getFileName())
                           .append(" - ").append(f.reason()).append("\n"));
                if (result.getFailedCount() > 10) {
                    message.append("... and ").append(result.getFailedCount() - 10).append(" more\n");
                }
                showWarning(message.toString(), "Delete Partially Complete");
            } else {
                JOptionPane.showMessageDialog(this,
                    String.format("Successfully deleted %d files (%s).",
                        result.getDeletedCount(), sizeText),
                    "Delete Complete", JOptionPane.INFORMATION_MESSAGE);
            }

            updateButtonStates();
        });
    }

    @Override
    public void deleteFailed(String error) {
        SwingUtilities.invokeLater(() -> {
            scanProgressBar.setString("Delete failed: " + error);
            statusLabel.setText("Delete failed");
            deleteSelectedButton.setText("Delete Selected");
            updateButtonStates();

            if (!error.toLowerCase().contains("cancel")) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + error,
                    "Delete Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ====== OPERACJE HASH STORAGE ======

    private void initializeHashStorage() {
        if (configuration.getMasterBackupLocation() == null ||
            !configuration.getMasterBackupLocation().exists()) {
            return;
        }

        hashStorageService = new HashStorageService(configuration.getMasterBackupLocation(),
            configuration.getHashingThreadCount());

        SwingWorker<HashStorageService.ValidationResult, String> validator = new SwingWorker<>() {
            @Override
            protected HashStorageService.ValidationResult doInBackground() throws Exception {
                MultiThreadedHashCalculator.ProgressCallback progressCallback =
                    (current, total, currentFile, _) -> {
                        setProgress(Math.min((current * 100) / Math.max(1, total), 100));
                        publish("Validating: " + currentFile + " (" + current + "/" + total + ")");
                    };
                return hashStorageService.validateAndUpdateHashesMultiThreaded(progressCallback, null);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    statusLabel.setText(chunks.getLast());
                }
            }

            @Override
            protected void done() {
                try {
                    HashStorageService.ValidationResult result = get();
                    if (result.hasChanges()) {
                        statusLabel.setText("Master folder updated: " + result.getTotalChanges() + " changes");
                        if (result.getTotalChanges() > 10) {
                            String message = String.format("""
                                Master folder validation completed:
                                New files: %d
                                Modified files: %d
                                Deleted files: %d""",
                                result.getNewFiles().size(),
                                result.getModifiedFiles().size(),
                                result.getDeletedFiles().size());
                            JOptionPane.showMessageDialog(MainWindow.this, message,
                                    "Master Folder Updated", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        statusLabel.setText("Master folder is up to date");
                    }
                } catch (Exception e) {
                    statusLabel.setText("Hash validation failed: " + e.getMessage());
                }
            }
        };
        validator.execute();
    }

    private void viewDuplicates() {
        if (lastDuplicateResult != null) {
            new DuplicateViewerWindow(this, lastDuplicateResult).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "No duplicate analysis available. Please run a scan with duplicate detection enabled first.",
                    "No Duplicate Data", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void loadSavedConfiguration() {
        if (configuration.getMasterBackupLocation() != null) {
            masterLocationLabel.setText(configuration.getMasterBackupLocation().getAbsolutePath());
        }

        sourceListModel.clear();
        configuration.getSourceDirectories().forEach(sourceListModel::addElement);

        syncListModel.clear();
        configuration.getSyncLocations().forEach(syncListModel::addElement);

        includeSubdirectoriesCheckBox.setSelected(configuration.isIncludeSubdirectories());
        createDateFoldersCheckBox.setSelected(configuration.isCreateDateFolders());
        skipHashingCheckBox.setSelected(configuration.isSkipHashing());
        threadCountSpinner.setValue(configuration.getHashingThreadCount());

        updateHashingControlsState();

        if (configPersistenceService.hasConfiguration()) {
            statusLabel.setText("Configuration loaded from: " + configPersistenceService.getConfigurationPath());
        }
    }

    private void saveConfiguration() {
        configPersistenceService.saveConfiguration(configuration);
    }

    private void rescanMasterFolder() {
        if (configuration.getMasterBackupLocation() == null) {
            showWarning("No master backup location configured.", "Configuration Error");
            return;
        }

        if (hashStorageService == null) {
            initializeHashStorage();
        }

        if (!confirmAction("""
                This will rescan the entire master backup folder and update the hash database.
                This may take some time depending on the number of files.
                
                Do you want to continue?""", "Confirm Master Folder Rescan")) {
            return;
        }

        rescanMasterButton.setEnabled(false);
        scanButton.setEnabled(false);
        backupButton.setEnabled(false);
        statusLabel.setText("Rescanning master backup folder...");
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Rescanning master folder...");

        createRescanWorker(false).execute();
    }

    private void performAutomaticMasterRescan() {
        if (configuration.getMasterBackupLocation() == null || hashStorageService == null) {
            return;
        }

        scanProgressBar.setValue(0);
        scanProgressBar.setString("Refreshing master folder...");

        createRescanWorker(true).execute();
    }

    private SwingWorker<HashStorageService.ValidationResult, String> createRescanWorker(boolean isAutomatic) {
        return new SwingWorker<>() {
            @Override
            protected HashStorageService.ValidationResult doInBackground() throws Exception {
                MultiThreadedHashCalculator.ProgressCallback progressCallback =
                    (current, total, currentFile, _) -> {
                        if (!isCancelled()) {
                            setProgress(Math.min((current * 100) / Math.max(1, total), 100));
                            if (current >= total && currentFile.startsWith("Completed in ")) {
                                lastTimingInfo = currentFile;
                                publish("Master folder " + (isAutomatic ? "refresh " : "rescan ") + currentFile);
                            } else {
                                publish((isAutomatic ? "Refreshing: " : "Processing: ") + current + "/" + total);
                            }
                        }
                    };
                return hashStorageService.forceRehashMultiThreaded(progressCallback, this::isCancelled);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String lastMessage = chunks.getLast();
                    scanProgressBar.setString(lastMessage);
                    if (!isAutomatic) {
                        statusLabel.setText("Rescanning master folder: " + lastMessage);
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    HashStorageService.ValidationResult result = get();
                    scanProgressBar.setValue(100);

                    String timingInfo = result.getProcessingTimeMs() > 0
                        ? "completed in " + result.getFormattedDuration() +
                          " (" + String.format("%.1f", result.getThroughputMbPerSec()) + " MB/s)"
                        : (isAutomatic ? "refreshed" : "completed");

                    scanProgressBar.setString("Master folder " + (isAutomatic ? "refresh " : "rescan ") + timingInfo);
                    statusLabel.setText("Master folder " + (isAutomatic ? "refreshed" : "rescan completed") +
                                       ": " + result.getTotalChanges() + " changes detected");

                    if (!isAutomatic) {
                        String message = String.format("""
                            Master folder rescan completed successfully!
                            
                            Files processed:
                            • New files: %d
                            • Modified files: %d
                            • Removed files: %d
                            
                            Total changes: %d""",
                            result.getNewFiles().size(),
                            result.getModifiedFiles().size(),
                            result.getDeletedFiles().size(),
                            result.getTotalChanges());
                        JOptionPane.showMessageDialog(MainWindow.this, message,
                                "Rescan Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    scanProgressBar.setString((isAutomatic ? "Refresh" : "Rescan") + " failed");
                    statusLabel.setText("Master folder " + (isAutomatic ? "refresh" : "rescan") +
                                       " failed: " + e.getMessage());
                    if (!isAutomatic) {
                        JOptionPane.showMessageDialog(MainWindow.this,
                                "Failed to rescan master folder:\n" + e.getMessage(),
                                "Rescan Error", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    updateButtonStates();
                }
            }
        };
    }

    // ====== OBSLUGA ZMIANY JEZYKA ======

    @Override
    public void onLanguageChanged(Locale newLocale) {
        SwingUtilities.invokeLater(this::updateUITexts);
    }

    private void updateUITexts() {
        // Tytul okna
        setTitle(get("app.title"));

        // Panel konfiguracji
        configPanel.setBorder(createTitledBorder(get("config.title")));
        masterLocationTitle.setText(get("config.masterLocation"));
        if (configuration.getMasterBackupLocation() == null) {
            masterLocationLabel.setText(get("config.noLocationSelected"));
        }
        sourceDirectoriesTitle.setText(get("config.sourceDirectories"));
        syncLocationsTitle.setText(get("config.syncLocations"));
        syncLocationsTitle.setToolTipText(get("config.syncLocationsTooltip"));

        // Przyciski nawigacji
        browseMasterButton.setText(get("button.browse"));
        addSourceButton.setText(get("button.add"));
        removeSourceButton.setText(get("button.remove"));
        addSyncButton.setText(get("button.add"));
        removeSyncButton.setText(get("button.remove"));

        // Opcje
        includeSubdirectoriesCheckBox.setText(get("option.includeSubdirectories"));
        createDateFoldersCheckBox.setText(get("option.createDateFolders"));
        enableDuplicateDetectionCheckBox.setText(get("option.detectDuplicates"));
        skipHashingCheckBox.setText(get("option.skipHashing"));
        skipHashingCheckBox.setToolTipText(get("option.skipHashingTooltip"));
        hashThreadsLabel.setText(get("option.hashThreads"));

        // Przyciski akcji - aktualizuj tylko jezeli nie sa w trakcie operacji
        if (!isScanInProgress()) {
            scanButton.setText(get("button.scanFiles"));
        }
        if (!isBackupInProgress()) {
            backupButton.setText(get("button.startBackup"));
        }
        viewDuplicatesButton.setText(get("button.viewDuplicates"));
        rescanMasterButton.setText(get("button.rescanMaster"));
        if (!isSyncInProgress()) {
            syncButton.setText(get("button.syncLocations"));
        }
        if (!isDeleteInProgress()) {
            deleteSelectedButton.setText(get("button.deleteSelected"));
        }

        // Panel postepu
        progressPanel.setBorder(createTitledBorder(get("progress.title")));

        // Aktualizuj FileListPanel
        fileListPanel.updateLanguage();

        // Wymus ponowne obliczenie layoutu wszystkich paneli
        configPanel.revalidate();
        progressPanel.revalidate();

        // Odswiez cale okno
        getContentPane().revalidate();
        getContentPane().repaint();

        // Dostosuj rozmiar okna do nowej zawartosci
        SwingUtilities.invokeLater(() -> {
            Dimension minSize = getMinimumSize();
            pack();
            Dimension packedSize = getSize();

            // Upewnij sie ze okno nie jest mniejsze niz minimum
            int newWidth = Math.max(packedSize.width, minSize.width);
            int newHeight = Math.max(packedSize.height, minSize.height);
            setSize(newWidth, newHeight);

            // Jesli okno wyszlo poza ekran, wycentruj je
            setLocationRelativeTo(null);
        });
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            LanguageManager.removeLanguageChangeListener(this);
            saveConfiguration();
        }
        super.processWindowEvent(e);
    }
}
