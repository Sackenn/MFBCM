package org.example.gui;

import org.example.model.BackupConfiguration;
import org.example.model.BackupFile;
import org.example.service.FileScanner;
import org.example.service.BackupService;
import org.example.service.HashStorageService;
import org.example.service.DuplicateDetectionService;
import org.example.service.DuplicateAnalysisResult;
import org.example.service.ConfigurationPersistenceService;
import org.example.service.SyncService;
import org.example.service.SyncResult;
import org.example.util.MultiThreadedHashCalculator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Main window of the Multimedia File Backup Manager application.
 */
public class MainWindow extends JFrame implements FileScanner.ScanProgressCallback,
                                                 BackupService.BackupProgressCallback,
                                                 DuplicateDetectionService.DuplicateDetectionCallback,
                                                 SyncService.SyncProgressCallback {

    private final BackupConfiguration configuration;
    private HashStorageService hashStorageService;
    private final ConfigurationPersistenceService configPersistenceService;

    // UI Components
    private JLabel masterLocationLabel;
    private JButton browseMasterButton;
    private JList<File> sourceDirectoriesList;
    private DefaultListModel<File> sourceListModel;
    private JButton addSourceButton;
    private JButton removeSourceButton;
    private JList<File> syncLocationsList;
    private DefaultListModel<File> syncListModel;
    private JButton addSyncButton;
    private JButton removeSyncButton;
    private JButton syncButton;
    private JButton scanButton;
    private JButton backupButton;
    private JButton viewDuplicatesButton;
    private JButton rescanMasterButton;

    private FileListPanel fileListPanel;
    private JProgressBar scanProgressBar;
    private JProgressBar backupProgressBar;
    private JProgressBar syncProgressBar;
    private JLabel statusLabel;

    private JCheckBox includeSubdirectoriesCheckBox;
    private JCheckBox createDateFoldersCheckBox;
    private JCheckBox enableDuplicateDetectionCheckBox;
    private JSpinner threadCountSpinner;

    private FileScanner currentScanner;
    private BackupService currentBackupService;
    private DuplicateDetectionService currentDuplicateService;
    private SyncService currentSyncService;
    private DuplicateAnalysisResult lastDuplicateResult;
    private String lastTimingInfo;

    public MainWindow() {
        this.configPersistenceService = new ConfigurationPersistenceService();
        this.configuration = configPersistenceService.loadConfiguration();
        initializeUI();
        setupEventHandlers();
        loadSavedConfiguration();
        initializeHashStorage();
    }

    private void initializeUI() {
        setTitle("Multimedia File Backup Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Modern window decoration
        getRootPane().putClientProperty("JRootPane.titleBarBackground", new Color(40, 44, 52));
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.WHITE);

        // Create main panels with spacing
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainContainer.add(createConfigurationPanel(), BorderLayout.NORTH);
        mainContainer.add(createCenterPanel(), BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);

        // Set initial state
        updateButtonStates();

        // Window settings - responsive sizing
        setMinimumSize(new Dimension(1000, 750));
        setPreferredSize(new Dimension(1200, 1000));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createConfigurationPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                "Configuration",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(200, 200, 200)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        GridBagConstraints gbc = new GridBagConstraints();

        // Master backup location
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 8, 8, 8);
        JLabel masterLabel = new JLabel("Master Backup Location:");
        masterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(masterLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        masterLocationLabel = new JLabel("No location selected");
        masterLocationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        masterLocationLabel.setOpaque(true);
        masterLocationLabel.setBackground(new Color(50, 54, 62));
        masterLocationLabel.setForeground(new Color(180, 180, 180));
        masterLocationLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 74, 82), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        panel.add(masterLocationLabel, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        browseMasterButton = new JButton("Browse...");
        browseMasterButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        browseMasterButton.setFocusPainted(false);
        panel.add(browseMasterButton, gbc);

        // Source directories
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel sourceLabel = new JLabel("Source Directories:");
        sourceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(sourceLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        sourceListModel = new DefaultListModel<>();
        sourceDirectoriesList = new JList<>(sourceListModel);
        sourceDirectoriesList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sourceDirectoriesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sourceDirectoriesList.setBackground(new Color(50, 54, 62));
        sourceDirectoriesList.setForeground(new Color(200, 200, 200));
        JScrollPane sourceScrollPane = new JScrollPane(sourceDirectoriesList);
        sourceScrollPane.setPreferredSize(new Dimension(400, 60));
        sourceScrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 74, 82), 1));
        panel.add(sourceScrollPane, gbc);

        // Source directory buttons
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JPanel sourceButtonPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        sourceButtonPanel.setOpaque(false);
        addSourceButton = new JButton("Add...");
        addSourceButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addSourceButton.setFocusPainted(false);
        removeSourceButton = new JButton("Remove");
        removeSourceButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        removeSourceButton.setFocusPainted(false);
        sourceButtonPanel.add(addSourceButton);
        sourceButtonPanel.add(removeSourceButton);
        panel.add(sourceButtonPanel, gbc);

        // Sync locations
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weighty = 1.0;
        JLabel syncLabel = new JLabel("Sync Locations:");
        syncLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        syncLabel.setToolTipText("Create copies of master folder in these locations");
        panel.add(syncLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        syncListModel = new DefaultListModel<>();
        syncLocationsList = new JList<>(syncListModel);
        syncLocationsList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        syncLocationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        syncLocationsList.setBackground(new Color(50, 54, 62));
        syncLocationsList.setForeground(new Color(200, 200, 200));
        JScrollPane syncScrollPane = new JScrollPane(syncLocationsList);
        syncScrollPane.setPreferredSize(new Dimension(400, 60));
        syncScrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 74, 82), 1));
        panel.add(syncScrollPane, gbc);

        // Sync location buttons
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        JPanel syncButtonPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        syncButtonPanel.setOpaque(false);
        addSyncButton = new JButton("Add...");
        addSyncButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addSyncButton.setFocusPainted(false);
        removeSyncButton = new JButton("Remove");
        removeSyncButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        removeSyncButton.setFocusPainted(false);
        syncButtonPanel.add(addSyncButton);
        syncButtonPanel.add(removeSyncButton);
        panel.add(syncButtonPanel, gbc);

        // Options
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        optionsPanel.setOpaque(false);

        includeSubdirectoriesCheckBox = new JCheckBox("Include subdirectories", true);
        includeSubdirectoriesCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        includeSubdirectoriesCheckBox.setFocusPainted(false);

        createDateFoldersCheckBox = new JCheckBox("Create date-based folders", false);
        createDateFoldersCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        createDateFoldersCheckBox.setFocusPainted(false);

        enableDuplicateDetectionCheckBox = new JCheckBox("Detect duplicates with master folder", true);
        enableDuplicateDetectionCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        enableDuplicateDetectionCheckBox.setFocusPainted(false);

        optionsPanel.add(includeSubdirectoriesCheckBox);
        optionsPanel.add(createDateFoldersCheckBox);
        optionsPanel.add(enableDuplicateDetectionCheckBox);

        JLabel threadLabel = new JLabel("Hash threads:");
        threadLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        optionsPanel.add(threadLabel);
        threadCountSpinner = new JSpinner(new SpinnerNumberModel(
            Runtime.getRuntime().availableProcessors(),
            1,
            Runtime.getRuntime().availableProcessors() * 2,
            1
        ));
        threadCountSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        threadCountSpinner.setPreferredSize(new Dimension(70, 28));
        optionsPanel.add(threadCountSpinner);
        panel.add(optionsPanel, gbc);

        // Action buttons with modern styling
        gbc.gridy = 4;
        gbc.insets = new Insets(6, 8, 4, 8);
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actionPanel.setOpaque(false);

        scanButton = createStyledButton("Scan for Files");
        backupButton = createStyledButton("Start Backup");
        viewDuplicatesButton = createStyledButton("View Duplicates");
        viewDuplicatesButton.setEnabled(false);
        rescanMasterButton = createStyledButton("Rescan Master");
        rescanMasterButton.setEnabled(false);
        syncButton = createStyledButton("Sync Locations");
        syncButton.setEnabled(false);

        actionPanel.add(scanButton);
        actionPanel.add(backupButton);
        actionPanel.add(viewDuplicatesButton);
        actionPanel.add(rescanMasterButton);
        actionPanel.add(syncButton);
        panel.add(actionPanel, gbc);

        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(150, 36));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // File list panel
        fileListPanel = new FileListPanel();
        panel.add(fileListPanel, BorderLayout.CENTER);

        // Progress bars with modern styling
        JPanel progressPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        progressPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                "Progress",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(200, 200, 200)
            ),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        scanProgressBar = new JProgressBar(0, 100);
        scanProgressBar.setStringPainted(true);
        scanProgressBar.setString("Ready to scan");
        scanProgressBar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        scanProgressBar.setPreferredSize(new Dimension(0, 28));
        progressPanel.add(scanProgressBar);

        backupProgressBar = new JProgressBar(0, 100);
        backupProgressBar.setStringPainted(true);
        backupProgressBar.setString("Ready to backup");
        backupProgressBar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backupProgressBar.setPreferredSize(new Dimension(0, 28));
        progressPanel.add(backupProgressBar);

        syncProgressBar = new JProgressBar(0, 100);
        syncProgressBar.setStringPainted(true);
        syncProgressBar.setString("Ready to sync");
        syncProgressBar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        syncProgressBar.setPreferredSize(new Dimension(0, 28));
        progressPanel.add(syncProgressBar);

        panel.add(progressPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 44, 52));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 74, 82)),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(180, 180, 180));
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
    }

    private void setupEventHandlers() {
        browseMasterButton.addActionListener(_ -> browseMasterLocation());
        addSourceButton.addActionListener(_ -> addSourceDirectory());
        removeSourceButton.addActionListener(_ -> removeSourceDirectory());
        addSyncButton.addActionListener(_ -> addSyncLocation());
        removeSyncButton.addActionListener(_ -> removeSyncLocation());
        scanButton.addActionListener(_ -> startScan());
        backupButton.addActionListener(_ -> startBackup());
        viewDuplicatesButton.addActionListener(_ -> viewDuplicates());
        rescanMasterButton.addActionListener(_ -> rescanMasterFolder());
        syncButton.addActionListener(_ -> startSync());

        includeSubdirectoriesCheckBox.addActionListener(_ -> {
            configuration.setIncludeSubdirectories(includeSubdirectoriesCheckBox.isSelected());
            saveConfiguration();
        });

        createDateFoldersCheckBox.addActionListener(_ -> {
            configuration.setCreateDateFolders(createDateFoldersCheckBox.isSelected());
            saveConfiguration();
        });

        threadCountSpinner.addChangeListener(_ -> {
            configuration.setHashingThreadCount((Integer) threadCountSpinner.getValue());
            saveConfiguration();
        });

        sourceDirectoriesList.addListSelectionListener(_ -> updateButtonStates());
        syncLocationsList.addListSelectionListener(_ -> updateButtonStates());
    }

    private void browseMasterLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Master Backup Location");

        if (configuration.getMasterBackupLocation() != null) {
            chooser.setCurrentDirectory(configuration.getMasterBackupLocation());
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
            configuration.setMasterBackupLocation(selectedDir);
            masterLocationLabel.setText(selectedDir.getAbsolutePath());
            initializeHashStorage();
            updateButtonStates();
            saveConfiguration();
        }
    }

    private void addSourceDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Source Directory");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();
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
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Sync Location");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = chooser.getSelectedFile();

            // Check if it's the same as master location
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

    private void startScan() {
        // Check if we should cancel an existing scan
        boolean scanInProgress = (currentScanner != null && !currentScanner.isDone()) ||
                                (currentDuplicateService != null && !currentDuplicateService.isDone());

        if (scanInProgress) {
            // Cancel the current scan
            if (currentScanner != null && !currentScanner.isDone()) {
                currentScanner.cancel(true);
                currentScanner = null;
            }
            if (currentDuplicateService != null && !currentDuplicateService.isDone()) {
                currentDuplicateService.cancel(true);
                currentDuplicateService = null;
            }

            // Reset UI
            scanProgressBar.setValue(0);
            scanProgressBar.setString("Scan cancelled");
            statusLabel.setText("Scan cancelled by user");
            updateButtonStates();
            return;
        }

        // Start new scan
        fileListPanel.clearFiles();
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Starting scan...");

        if (enableDuplicateDetectionCheckBox.isSelected() && hashStorageService != null) {
            // Use duplicate detection service
            currentDuplicateService = new DuplicateDetectionService(configuration, hashStorageService, this);
            currentDuplicateService.execute();
        } else {
            // Use regular scanner
            currentScanner = new FileScanner(configuration, this);
            currentScanner.execute();
        }

        scanButton.setText("Cancel Scan");
        backupButton.setEnabled(false);
        viewDuplicatesButton.setEnabled(false);
    }

    private void startBackup() {
        // Check if we should cancel an existing backup
        boolean backupInProgress = currentBackupService != null && !currentBackupService.isDone();

        if (backupInProgress) {
            // Cancel the current backup
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel the backup?",
                    "Cancel Backup", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                currentBackupService.cancel(true);
                currentBackupService = null;

                // Reset UI
                backupProgressBar.setValue(0);
                backupProgressBar.setString("Backup cancelled");
                statusLabel.setText("Backup cancelled by user");
                updateButtonStates();
            }
            return;
        }

        // Start new backup
        List<BackupFile> selectedFiles = fileListPanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files selected for backup.",
                    "No Files Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(this,
                "Start backup of " + selectedFiles.size() + " files?",
                "Confirm Backup", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            backupProgressBar.setValue(0);
            backupProgressBar.setString("Starting backup...");

            currentBackupService = new BackupService(fileListPanel.getAllFiles(), configuration, this);
            currentBackupService.execute();

            backupButton.setText("Cancel Backup");
            scanButton.setEnabled(false);
        }
    }

    private void startSync() {
        // Check if we should cancel an existing sync
        boolean syncInProgress = currentSyncService != null && !currentSyncService.isDone();

        if (syncInProgress) {
            // Cancel the current sync
            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to cancel the sync?",
                    "Cancel Sync", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                currentSyncService.cancel(true);
                currentSyncService = null;

                // Reset UI
                syncProgressBar.setValue(0);
                syncProgressBar.setString("Sync cancelled");
                statusLabel.setText("Sync cancelled by user");
                updateButtonStates();
            }
            return;
        }

        // Confirm sync operation
        List<File> syncLocations = configuration.getSyncLocations();
        StringBuilder message = new StringBuilder();
        message.append("Sync master folder to the following locations?\n\n");
        for (File location : syncLocations) {
            message.append("• ").append(location.getAbsolutePath()).append("\n");
        }
        message.append("\nThis will copy all files from the master folder and remove files that no longer exist.");

        int result = JOptionPane.showConfirmDialog(this,
                message.toString(),
                "Confirm Sync",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            syncProgressBar.setValue(0);
            syncProgressBar.setString("Starting sync...");
            statusLabel.setText("Syncing master folder...");

            currentSyncService = new SyncService(configuration, this);
            currentSyncService.execute();

            syncButton.setText("Cancel Sync");
            updateButtonStates();
        }
    }

    private void updateButtonStates() {
        removeSourceButton.setEnabled(sourceDirectoriesList.getSelectedValue() != null);
        removeSyncButton.setEnabled(syncLocationsList.getSelectedValue() != null);

        boolean canScan = configuration.getMasterBackupLocation() != null &&
                         !configuration.getSourceDirectories().isEmpty();
        boolean scanInProgress = (currentScanner != null && !currentScanner.isDone()) ||
                                (currentDuplicateService != null && !currentDuplicateService.isDone());

        scanButton.setEnabled(canScan);
        scanButton.setText(scanInProgress ? "Cancel Scan" : "Scan for Files");

        boolean hasFiles = fileListPanel.getFileCount() > 0;
        boolean backupInProgress = currentBackupService != null && !currentBackupService.isDone();

        backupButton.setEnabled(hasFiles && !scanInProgress && canScan);
        backupButton.setText(backupInProgress ? "Cancel Backup" : "Start Backup");

        // View duplicates button is enabled when we have duplicate results
        if (!viewDuplicatesButton.isEnabled() && lastDuplicateResult != null &&
            lastDuplicateResult.getTotalDuplicateCount() > 0) {
            viewDuplicatesButton.setEnabled(true);
        }

        // Rescan master button is enabled whenever master location is set (creates hash storage if needed)
        boolean canRescanMaster = configuration.getMasterBackupLocation() != null &&
                                 !scanInProgress && !backupInProgress;
        rescanMasterButton.setEnabled(canRescanMaster);

        // Sync button is enabled when master location is set and there are sync locations
        boolean syncInProgress = currentSyncService != null && !currentSyncService.isDone();
        boolean canSync = configuration.getMasterBackupLocation() != null &&
                         !configuration.getSyncLocations().isEmpty() &&
                         !scanInProgress && !backupInProgress && !syncInProgress;
        syncButton.setEnabled(canSync);
        syncButton.setText(syncInProgress ? "Cancel Sync" : "Sync Locations");
    }

    // FileScanner.ScanProgressCallback and DuplicateDetectionCallback implementation
    @Override
    public void updateProgress(int current, int total, String currentFile) {
        SwingUtilities.invokeLater(() -> {
            if (total > 0) {
                int percentage = (current * 100) / total;
                scanProgressBar.setValue(percentage);
            }

            // Determine which service is running to show appropriate message
            boolean isDuplicateDetection = (currentDuplicateService != null && !currentDuplicateService.isDone());

            if (current >= total && currentFile.startsWith("Completed in ")) {
                // Store timing information for use in completion callbacks
                lastTimingInfo = currentFile;
                // Final progress update with timing information from MultiThreadedHashCalculator
                if (isDuplicateDetection) {
                    scanProgressBar.setString("Duplicate detection " + currentFile);
                } else {
                    scanProgressBar.setString("Scan " + currentFile);
                }
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
            if (lastTimingInfo != null) {
                scanProgressBar.setString("Scan " + lastTimingInfo + " - " + files.size() + " files found");
                lastTimingInfo = null; // Clear after use
            } else {
                scanProgressBar.setString("Scan completed - " + files.size() + " files found");
            }
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

    // BackupService.BackupProgressCallback and SyncService.SyncProgressCallback implementation
    // Note: Both interfaces have the same method signature, so this implementation serves both
    @Override
    public void updateProgress(int current, int total, String currentFile,
                              long bytesProcessed, long totalBytes) {
        SwingUtilities.invokeLater(() -> {
            if (total > 0) {
                int percentage = (current * 100) / total;

                // Determine which operation is running
                boolean isBackup = currentBackupService != null && !currentBackupService.isDone();
                boolean isSync = currentSyncService != null && !currentSyncService.isDone();

                if (isBackup) {
                    backupProgressBar.setValue(percentage);
                    backupProgressBar.setString("Backing up: " + currentFile);

                    String bytesText = "";
                    if (totalBytes > 0) {
                        bytesText = String.format(" (%.1f%% of data)", (bytesProcessed * 100.0) / totalBytes);
                    }
                    statusLabel.setText("Backed up " + current + " of " + total + " files" + bytesText);
                } else if (isSync) {
                    syncProgressBar.setValue(percentage);
                    syncProgressBar.setString("Syncing: " + currentFile);

                    String bytesText = "";
                    if (totalBytes > 0) {
                        bytesText = String.format(" (%.1f%% of data)", (bytesProcessed * 100.0) / totalBytes);
                    }
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

            // Automatically rescan master folder if backup was successful
            if (successCount > 0) {
                statusLabel.setText("Refreshing master folder...");
                // Trigger automatic rescan without confirmation dialog
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
                if (filesBackedUpBeforeCancellation > 0) {
                    statusLabel.setText("Backup cancelled - " + filesBackedUpBeforeCancellation + " files were copied before cancellation");
                } else {
                    statusLabel.setText("Backup cancelled by user");
                }

                // Trigger rescan if any files were copied before cancellation
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

    // This method handles both FileScanner and DuplicateDetection progress updates
    // The implementation is the same but displays different messages based on which service is running

    @Override
    public void detectionCompleted(DuplicateAnalysisResult result) {
        SwingUtilities.invokeLater(() -> {
            this.lastDuplicateResult = result;
            fileListPanel.setFiles(result.getSourceFiles());
            scanProgressBar.setValue(100);
            if (result.getProcessingTimeMs() > 0) {
                String timingInfo = "completed in " + result.getFormattedDuration() +
                                  " (" + String.format("%.1f", result.getThroughputMbPerSec()) + " MB/s)";
                scanProgressBar.setString("Duplicate detection " + timingInfo + " - " + result.getTotalSourceFiles() + " files found");
            } else {
                scanProgressBar.setString("Duplicate detection completed - " + result.getTotalSourceFiles() + " files found");
            }
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

    // SyncService.SyncProgressCallback implementation
    @Override
    public void syncCompleted(SyncResult result) {
        SwingUtilities.invokeLater(() -> {
            syncProgressBar.setValue(100);
            syncProgressBar.setString("Sync completed - " + result.getSuccessCount() + " locations synced");
            statusLabel.setText("Sync completed: " + result.getSuccessCount() + " successful, " +
                              result.getFailureCount() + " failed");
            updateButtonStates();

            StringBuilder message = new StringBuilder();
            message.append("Sync completed!\n\n");

            if (!result.getSuccessfulLocations().isEmpty()) {
                message.append("Successfully synced to:\n");
                for (File location : result.getSuccessfulLocations()) {
                    message.append("• ").append(location.getAbsolutePath()).append("\n");
                }
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

    private void initializeHashStorage() {
        if (configuration.getMasterBackupLocation() != null &&
            configuration.getMasterBackupLocation().exists()) {

            hashStorageService = new HashStorageService(configuration.getMasterBackupLocation(),
                configuration.getHashingThreadCount());

            // Validate and update hashes on startup using multi-threaded approach
            SwingWorker<HashStorageService.ValidationResult, String> validator =
                new SwingWorker<>() {

                @Override
                protected HashStorageService.ValidationResult doInBackground() throws Exception {
                    // Progress callback for startup validation
                    MultiThreadedHashCalculator.ProgressCallback progressCallback =
                        (current, total, currentFile, _) -> {
                            int percentage = total > 0 ? (current * 100) / total : 0;
                            setProgress(Math.min(percentage, 100));
                            publish("Validating: " + currentFile + " (" + current + "/" + total + ")");
                        };

                    return hashStorageService.validateAndUpdateHashesMultiThreaded(progressCallback, null);
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    if (!chunks.isEmpty()) {
                        String lastMessage = chunks.getLast();
                        statusLabel.setText(lastMessage);
                    }
                }

                @Override
                protected void done() {
                    try {
                        HashStorageService.ValidationResult result = get();
                        if (result.hasChanges()) {
                            String message = String.format("""
                                Master folder validation completed:
                                New files: %d
                                Modified files: %d
                                Deleted files: %d""",
                                result.getNewFiles().size(),
                                result.getModifiedFiles().size(),
                                result.getDeletedFiles().size()
                            );

                            statusLabel.setText("Master folder updated: " + result.getTotalChanges() + " changes");

                            if (result.getTotalChanges() > 10) {
                                // Show summary for large changes
                                JOptionPane.showMessageDialog(MainWindow.this, message,
                                        "Master Folder Updated", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            statusLabel.setText("Master folder is up to date");
                        }
                    } catch (Exception e) {
                        statusLabel.setText("Hash validation failed: " + e.getMessage());
                        System.err.println("Hash validation error: " + e.getMessage());
                    }
                }
            };

            validator.execute();
        }
    }

    private void viewDuplicates() {
        if (lastDuplicateResult != null) {
            DuplicateViewerWindow viewer = new DuplicateViewerWindow(this, lastDuplicateResult);
            viewer.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "No duplicate analysis available. Please run a scan with duplicate detection enabled first.",
                    "No Duplicate Data", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Loads saved configuration into the UI components.
     */
    private void loadSavedConfiguration() {
        // Load master backup location
        if (configuration.getMasterBackupLocation() != null) {
            masterLocationLabel.setText(configuration.getMasterBackupLocation().getAbsolutePath());
        }

        // Load source directories
        sourceListModel.clear();
        for (File sourceDir : configuration.getSourceDirectories()) {
            sourceListModel.addElement(sourceDir);
        }

        // Load sync locations
        syncListModel.clear();
        for (File syncLocation : configuration.getSyncLocations()) {
            syncListModel.addElement(syncLocation);
        }

        // Load checkbox states
        includeSubdirectoriesCheckBox.setSelected(configuration.isIncludeSubdirectories());
        createDateFoldersCheckBox.setSelected(configuration.isCreateDateFolders());

        // Load thread count
        threadCountSpinner.setValue(configuration.getHashingThreadCount());

        // Show notification if configuration was loaded
        if (configPersistenceService.hasConfiguration()) {
            statusLabel.setText("Configuration loaded from: " + configPersistenceService.getConfigurationPath());
        }
    }

    /**
     * Saves current configuration to persistent storage.
     */
    private void saveConfiguration() {
        configPersistenceService.saveConfiguration(configuration);
    }

    /**
     * Rescans the master backup folder to update hash database.
     */
    private void rescanMasterFolder() {
        if (configuration.getMasterBackupLocation() == null) {
            JOptionPane.showMessageDialog(this,
                    "No master backup location configured.",
                    "Configuration Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Initialize hash storage if it doesn't exist
        if (hashStorageService == null) {
            initializeHashStorage();
        }

        int result = JOptionPane.showConfirmDialog(this,
                """
                This will rescan the entire master backup folder and update the hash database.
                This may take some time depending on the number of files.
                
                Do you want to continue?""",
                "Confirm Master Folder Rescan",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // Disable buttons during rescan
        rescanMasterButton.setEnabled(false);
        scanButton.setEnabled(false);
        backupButton.setEnabled(false);

        // Update status and progress
        statusLabel.setText("Rescanning master backup folder...");
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Rescanning master folder...");

        // Perform rescan in background thread with multi-threaded hashing
        SwingWorker<HashStorageService.ValidationResult, String> rescanWorker =
            new SwingWorker<>() {

            @Override
            protected HashStorageService.ValidationResult doInBackground() throws Exception {
                // Progress callback for multi-threaded hash calculation
                MultiThreadedHashCalculator.ProgressCallback progressCallback =
                    (current, total, currentFile, _) -> {
                        if (!isCancelled()) {
                            int percentage = total > 0 ? (current * 100) / total : 0;
                            setProgress(Math.min(percentage, 100));

                            if (current >= total && currentFile.startsWith("Completed in ")) {
                                // Store timing information for final message
                                lastTimingInfo = currentFile;
                                publish("Master folder rescan " + currentFile);
                            } else {
                                publish("Processing: " + currentFile + " (" + current + "/" + total + ")");
                            }
                        }
                    };

                // Cancellation check using built-in isCancelled method
                java.util.function.BooleanSupplier isCancelledSupplier = this::isCancelled;

                return hashStorageService.forceRehashMultiThreaded(progressCallback, isCancelledSupplier);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String lastMessage = chunks.getLast();
                    scanProgressBar.setString(lastMessage);
                    statusLabel.setText("Rescanning master folder: " + lastMessage);
                }
            }

            // Note: Cannot override cancel() as it's final, so we handle cancellation through the cancelled flag

            @Override
            protected void done() {
                try {
                    HashStorageService.ValidationResult result = get();

                    // Update UI with results
                    scanProgressBar.setValue(100);
                    if (result.getProcessingTimeMs() > 0) {
                        String timingInfo = "completed in " + result.getFormattedDuration() +
                                          " (" + String.format("%.1f", result.getThroughputMbPerSec()) + " MB/s)";
                        scanProgressBar.setString("Master folder rescan " + timingInfo);
                    } else {
                        scanProgressBar.setString("Master folder rescan completed");
                    }

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
                        result.getTotalChanges()
                    );

                    statusLabel.setText("Master folder rescan completed: " + result.getTotalChanges() + " changes detected");

                    JOptionPane.showMessageDialog(MainWindow.this, message,
                            "Rescan Complete", JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception e) {
                    scanProgressBar.setString("Rescan failed");
                    statusLabel.setText("Master folder rescan failed: " + e.getMessage());

                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Failed to rescan master folder:\n" + e.getMessage(),
                            "Rescan Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Re-enable buttons
                    updateButtonStates();
                }
            }
        };

        rescanWorker.execute();
    }

    /**
     * Performs an automatic rescan of the master folder without user confirmation.
     * Used after successful backups to update the hash database.
     */
    private void performAutomaticMasterRescan() {
        if (configuration.getMasterBackupLocation() == null || hashStorageService == null) {
            return;
        }

        // Update status and progress
        scanProgressBar.setValue(0);
        scanProgressBar.setString("Refreshing master folder...");

        // Perform rescan in background thread
        SwingWorker<HashStorageService.ValidationResult, String> rescanWorker =
            new SwingWorker<>() {

            @Override
            protected HashStorageService.ValidationResult doInBackground() throws Exception {
                // Progress callback for multi-threaded hash calculation
                MultiThreadedHashCalculator.ProgressCallback progressCallback =
                    (current, total, currentFile, _) -> {
                        if (!isCancelled()) {
                            int percentage = total > 0 ? (current * 100) / total : 0;
                            setProgress(Math.min(percentage, 100));

                            if (current >= total && currentFile.startsWith("Completed in ")) {
                                publish("Master folder refresh " + currentFile);
                            } else {
                                publish("Refreshing: " + current + "/" + total);
                            }
                        }
                    };

                java.util.function.BooleanSupplier isCancelledSupplier = this::isCancelled;
                return hashStorageService.forceRehashMultiThreaded(progressCallback, isCancelledSupplier);
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    String lastMessage = chunks.getLast();
                    scanProgressBar.setString(lastMessage);
                }
            }

            @Override
            protected void done() {
                try {
                    HashStorageService.ValidationResult result = get();
                    scanProgressBar.setValue(100);

                    if (result.getProcessingTimeMs() > 0) {
                        String timingInfo = "completed in " + result.getFormattedDuration() +
                                          " (" + String.format("%.1f", result.getThroughputMbPerSec()) + " MB/s)";
                        scanProgressBar.setString("Master folder refresh " + timingInfo);
                    } else {
                        scanProgressBar.setString("Master folder refreshed");
                    }

                    statusLabel.setText("Master folder refreshed: " + result.getTotalChanges() + " changes detected");
                } catch (Exception e) {
                    scanProgressBar.setString("Refresh failed");
                    statusLabel.setText("Master folder refresh failed: " + e.getMessage());
                } finally {
                    updateButtonStates();
                }
            }
        };

        rescanWorker.execute();
    }

    /**
     * Override window closing to save configuration before exit.
     */
    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            saveConfiguration();
        }
        super.processWindowEvent(e);
    }
}
