package org.example.gui;

import org.example.service.DuplicateAnalysisResult;
import org.example.service.DuplicatePair;
import org.example.model.BackupFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Window for viewing and managing duplicate files.
 */
public class DuplicateViewerWindow extends JDialog {

    private final DuplicateAnalysisResult analysisResult;
    private DuplicateTableModel tableModel;
    private JTable duplicateTable;
    private JLabel summaryLabel;
    private JTabbedPane tabbedPane;

    public DuplicateViewerWindow(JFrame parent, DuplicateAnalysisResult analysisResult) {
        super(parent, "Duplicate Files Viewer", true);
        this.analysisResult = analysisResult;

        initializeUI();
        populateData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        // Main container with padding
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create tabbed pane for different duplicate types with modern styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Duplicates in master folder
        JPanel masterDuplicatesPanel = createDuplicatesPanel("Files that already exist in master backup folder");
        tabbedPane.addTab("Master Duplicates (" + analysisResult.getDuplicateInMasterCount() + ")",
                         masterDuplicatesPanel);

        // Tab 2: Duplicates within source directories
        JPanel sourceDuplicatesPanel = createSourceDuplicatesPanel("Duplicate files found within source directories");
        tabbedPane.addTab("Source Duplicates (" + analysisResult.getDuplicateInSourceCount() + ")",
                         sourceDuplicatesPanel);

        // Tab 3: Summary
        JPanel summaryPanel = createSummaryPanel();
        tabbedPane.addTab("Summary", summaryPanel);

        mainContainer.add(tabbedPane, BorderLayout.CENTER);
        add(mainContainer, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);

        // Window settings - responsive sizing
        setMinimumSize(new Dimension(1000, 650));
        setPreferredSize(new Dimension(1200, 750));
        pack();
        setLocationRelativeTo(getParent());
    }

    private JPanel createDuplicatesPanel(String description) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Description label with modern styling
        JLabel descLabel = new JLabel("<html><i>" + description + "</i></html>");
        descLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        descLabel.setForeground(new Color(180, 180, 180));
        descLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        panel.add(descLabel, BorderLayout.NORTH);

        // Table for duplicate pairs
        tableModel = new DuplicateTableModel();
        duplicateTable = new JTable(tableModel);
        duplicateTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        duplicateTable.setRowHeight(28);
        duplicateTable.setShowGrid(false);
        duplicateTable.setIntercellSpacing(new Dimension(0, 0));
        duplicateTable.setSelectionBackground(new Color(70, 130, 180));
        duplicateTable.setSelectionForeground(Color.WHITE);
        setupTable();

        JScrollPane scrollPane = new JScrollPane(duplicateTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 74, 82), 1));
        scrollPane.getViewport().setBackground(new Color(45, 49, 57));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Actions panel with modern styling
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsPanel.setBackground(new Color(40, 44, 52));
        actionsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 74, 82)));

        JButton openSourceButton = createStyledButton("Open Source Location");
        openSourceButton.addActionListener(_ -> openSelectedSourceLocation());

        JButton openMasterButton = createStyledButton("Open Master Location");
        openMasterButton.addActionListener(_ -> openSelectedMasterLocation());

        JButton deleteSourceButton = createStyledButton("Delete Source File");
        deleteSourceButton.addActionListener(_ -> deleteSelectedSourceFile());

        actionsPanel.add(openSourceButton);
        actionsPanel.add(openMasterButton);
        actionsPanel.add(Box.createHorizontalStrut(10));
        actionsPanel.add(deleteSourceButton);

        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(180, 32));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    private JPanel createSourceDuplicatesPanel(String description) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Description label with modern styling
        JLabel descLabel = new JLabel("<html><i>" + description + " - Files with the same content within source directories</i></html>");
        descLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        descLabel.setForeground(new Color(180, 180, 180));
        descLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        panel.add(descLabel, BorderLayout.NORTH);

        // Create table for source duplicates
        SourceDuplicateTableModel sourceTableModel = new SourceDuplicateTableModel();
        JTable sourceDuplicateTable = new JTable(sourceTableModel);
        sourceDuplicateTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sourceDuplicateTable.setRowHeight(28);
        sourceDuplicateTable.setShowGrid(false);
        sourceDuplicateTable.setIntercellSpacing(new Dimension(0, 0));
        sourceDuplicateTable.setSelectionBackground(new Color(70, 130, 180));
        sourceDuplicateTable.setSelectionForeground(Color.WHITE);
        setupSourceDuplicateTable(sourceDuplicateTable, sourceTableModel);

        // Populate table with grouped source duplicates
        sourceTableModel.setDuplicateGroups(analysisResult.getSourceDuplicateGroups());

        JScrollPane scrollPane = new JScrollPane(sourceDuplicateTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 74, 82), 1));
        scrollPane.getViewport().setBackground(new Color(45, 49, 57));
        panel.add(scrollPane, BorderLayout.CENTER);

        // Actions for source duplicates with modern styling
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsPanel.setBackground(new Color(40, 44, 52));
        actionsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 74, 82)));

        JButton openLocationButton = createStyledButton("Open File Location");
        openLocationButton.addActionListener(_ -> {
            int selectedRow = sourceDuplicateTable.getSelectedRow();
            if (selectedRow >= 0) {
                BackupFile file = sourceTableModel.getFileAt(selectedRow);
                if (file != null) {
                    openFileLocation(file.getSourceFile());
                }
            }
        });

        JButton deleteFileButton = createStyledButton("Delete File");
        deleteFileButton.addActionListener(_ -> {
            int selectedRow = sourceDuplicateTable.getSelectedRow();
            if (selectedRow >= 0) {
                BackupFile file = sourceTableModel.getFileAt(selectedRow);
                if (file != null) {
                    deleteSourceDuplicateFile(file);
                }
            }
        });

        actionsPanel.add(openLocationButton);
        actionsPanel.add(Box.createHorizontalStrut(10));
        actionsPanel.add(deleteFileButton);
        panel.add(actionsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void setupSourceDuplicateTable(JTable table, SourceDuplicateTableModel model) {
        // Column widths for better proportions
        table.getColumnModel().getColumn(0).setPreferredWidth(60);  // Group
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(250); // File Name
        table.getColumnModel().getColumn(2).setPreferredWidth(450); // Path
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // Size
        table.getColumnModel().getColumn(4).setPreferredWidth(90);  // # in Group

        // Modern table header styling
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(50, 54, 62));
        table.getTableHeader().setForeground(new Color(200, 200, 200));
        table.getTableHeader().setReorderingAllowed(false);

        // Selection mode
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add custom cell renderer with group coloring
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            // Modern alternating colors for different groups
            private final Color[] groupColors = {
                new Color(55, 59, 67),
                new Color(60, 64, 72),
                new Color(50, 54, 62),
                new Color(58, 62, 70),
                new Color(52, 56, 64),
                new Color(56, 60, 68),
                new Color(54, 58, 66),
                new Color(57, 61, 69)
            };

            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                setFont(new Font("Segoe UI", Font.PLAIN, 12));

                // Set tooltip for long paths
                if (value != null) {
                    String text = value.toString();
                    setToolTipText(text.length() > 50 ? text : null);
                }

                // Center align Group and count columns
                if (column == 0 || column == 4) {
                    setHorizontalAlignment(CENTER);
                } else {
                    setHorizontalAlignment(LEFT);
                }

                // Apply alternating background colors for different groups
                if (!isSelected) {
                    int groupId = model.getGroupIdForRow(row);
                    Color bgColor = groupColors[(groupId - 1) % groupColors.length];
                    setBackground(bgColor);
                    setForeground(new Color(200, 200, 200));
                } else {
                    setBackground(tbl.getSelectionBackground());
                    setForeground(tbl.getSelectionForeground());
                }

                return c;
            }
        };

        // Apply renderer to all columns
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    private void deleteSourceDuplicateFile(BackupFile file) {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this file?\n\n" +
                file.getPath() + "\n\n" +
                "This action cannot be undone!",
                "Delete File",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            File sourceFile = file.getSourceFile();
            if (sourceFile.delete()) {
                JOptionPane.showMessageDialog(this, "File deleted successfully.",
                        "Delete Complete", JOptionPane.INFORMATION_MESSAGE);
                refreshData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete file.",
                        "Delete Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        Font valueFont = new Font("Segoe UI", Font.BOLD, 13);
        Color labelColor = new Color(180, 180, 180);
        Color valueColor = new Color(220, 220, 220);

        // Summary statistics with modern styling
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel label1 = new JLabel("Master Backup Files:");
        label1.setFont(labelFont);
        label1.setForeground(labelColor);
        panel.add(label1, gbc);
        gbc.gridx = 1;
        JLabel value1 = new JLabel(String.valueOf(analysisResult.getMasterFileCount()));
        value1.setFont(valueFont);
        value1.setForeground(valueColor);
        panel.add(value1, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel label2 = new JLabel("Total Source Files Found:");
        label2.setFont(labelFont);
        label2.setForeground(labelColor);
        panel.add(label2, gbc);
        gbc.gridx = 1;
        JLabel value2 = new JLabel(String.valueOf(analysisResult.getTotalSourceFiles()));
        value2.setFont(valueFont);
        value2.setForeground(valueColor);
        panel.add(value2, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel label3 = new JLabel("New Files (not in master):");
        label3.setFont(labelFont);
        label3.setForeground(labelColor);
        panel.add(label3, gbc);
        gbc.gridx = 1;
        JLabel value3 = new JLabel(String.valueOf(analysisResult.getNewFileCount()));
        value3.setFont(valueFont);
        value3.setForeground(new Color(46, 204, 113));
        panel.add(value3, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        JLabel label4 = new JLabel("Duplicates in Master:");
        label4.setFont(labelFont);
        label4.setForeground(labelColor);
        panel.add(label4, gbc);
        gbc.gridx = 1;
        JLabel value4 = new JLabel(String.valueOf(analysisResult.getDuplicateInMasterCount()));
        value4.setFont(valueFont);
        value4.setForeground(new Color(230, 126, 34));
        panel.add(value4, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        JLabel label5 = new JLabel("Duplicates in Source:");
        label5.setFont(labelFont);
        label5.setForeground(labelColor);
        panel.add(label5, gbc);
        gbc.gridx = 1;
        JLabel value5 = new JLabel(String.valueOf(analysisResult.getDuplicateInSourceCount()));
        value5.setFont(valueFont);
        value5.setForeground(new Color(230, 126, 34));
        panel.add(value5, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        JLabel label6 = new JLabel("Total Duplicates:");
        label6.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label6.setForeground(labelColor);
        panel.add(label6, gbc);
        gbc.gridx = 1;
        JLabel totalDuplicatesLabel = new JLabel(String.valueOf(analysisResult.getTotalDuplicateCount()));
        totalDuplicatesLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        totalDuplicatesLabel.setForeground(new Color(231, 76, 60));
        panel.add(totalDuplicatesLabel, gbc);

        // Space savings calculation
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.insets = new Insets(20, 12, 12, 12);
        JLabel label7 = new JLabel("Potential Space Savings:");
        label7.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label7.setForeground(labelColor);
        panel.add(label7, gbc);
        gbc.gridx = 1;
        long duplicateSize = analysisResult.getDuplicatesInMaster().stream()
                .mapToLong(BackupFile::getSize)
                .sum();
        duplicateSize += analysisResult.getDuplicatesInSource().stream()
                .mapToLong(BackupFile::getSize)
                .sum();
        JLabel value7 = new JLabel(formatSize(duplicateSize));
        value7.setFont(new Font("Segoe UI", Font.BOLD, 15));
        value7.setForeground(new Color(52, 152, 219));
        panel.add(value7, gbc);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(new Color(40, 44, 52));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 74, 82)));

        JButton refreshButton = createStyledButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(120, 32));
        refreshButton.addActionListener(_ -> refreshData());

        JButton closeButton = createStyledButton("Close");
        closeButton.setPreferredSize(new Dimension(120, 32));
        closeButton.addActionListener(_ -> dispose());

        panel.add(refreshButton);
        panel.add(closeButton);

        return panel;
    }

    private void setupTable() {
        // Column widths for better proportions
        duplicateTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Group
        duplicateTable.getColumnModel().getColumn(0).setMaxWidth(80);
        duplicateTable.getColumnModel().getColumn(1).setPreferredWidth(180); // Source File
        duplicateTable.getColumnModel().getColumn(2).setPreferredWidth(280); // Source Path
        duplicateTable.getColumnModel().getColumn(3).setPreferredWidth(180); // Master File
        duplicateTable.getColumnModel().getColumn(4).setPreferredWidth(280); // Master Path
        duplicateTable.getColumnModel().getColumn(5).setPreferredWidth(90);  // Size
        duplicateTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // Duplicate Count
        duplicateTable.getColumnModel().getColumn(7).setPreferredWidth(350); // All Duplicates

        // Modern table header styling
        duplicateTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        duplicateTable.getTableHeader().setBackground(new Color(50, 54, 62));
        duplicateTable.getTableHeader().setForeground(new Color(200, 200, 200));
        duplicateTable.getTableHeader().setReorderingAllowed(false);

        // Selection mode
        duplicateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add custom cell renderer with tooltip support and group coloring
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            // Modern alternating colors for different groups
            private final Color[] groupColors = {
                new Color(55, 59, 67),
                new Color(60, 64, 72),
                new Color(50, 54, 62),
                new Color(58, 62, 70),
                new Color(52, 56, 64),
                new Color(56, 60, 68),
                new Color(54, 58, 66),
                new Color(57, 61, 69)
            };

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                setFont(new Font("Segoe UI", Font.PLAIN, 12));

                // Set tooltip to show full text for long paths
                if (value != null) {
                    String text = value.toString();
                    setToolTipText(text.length() > 50 ? text : null);
                }

                // Center align the Group column and duplicate count column
                if (column == 0 || column == 6) {
                    setHorizontalAlignment(CENTER);
                } else {
                    setHorizontalAlignment(LEFT);
                }

                // Apply alternating background colors for different groups
                if (!isSelected) {
                    int groupId = tableModel.getGroupIdForRow(row);
                    Color bgColor = groupColors[(groupId - 1) % groupColors.length];
                    setBackground(bgColor);
                    setForeground(new Color(200, 200, 200));
                } else {
                    setBackground(table.getSelectionBackground());
                    setForeground(table.getSelectionForeground());
                }

                return c;
            }
        };

        // Apply renderer to all columns
        for (int i = 0; i < duplicateTable.getColumnCount(); i++) {
            duplicateTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    private void populateData() {
        tableModel.setDuplicatePairs(analysisResult.getDuplicatePairs());
    }

    private void refreshData() {
        // This could trigger a new duplicate scan if needed
        populateData();
    }

    private void openSelectedSourceLocation() {
        int selectedRow = duplicateTable.getSelectedRow();
        if (selectedRow >= 0) {
            DuplicatePair pair = tableModel.getDuplicatePairAt(selectedRow);
            openFileLocation(pair.getSourceFile().getSourceFile());
        }
    }

    private void openSelectedMasterLocation() {
        int selectedRow = duplicateTable.getSelectedRow();
        if (selectedRow >= 0) {
            DuplicatePair pair = tableModel.getDuplicatePairAt(selectedRow);
            openFileLocation(pair.getMasterFile());
        }
    }

    private void deleteSelectedSourceFile() {
        int selectedRow = duplicateTable.getSelectedRow();
        if (selectedRow >= 0) {
            DuplicatePair pair = tableModel.getDuplicatePairAt(selectedRow);

            int result = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete the source file?\n\n" +
                    pair.getSourcePath() + "\n\n" +
                    "This action cannot be undone!",
                    "Delete Source File",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                File sourceFile = pair.getSourceFile().getSourceFile();
                if (sourceFile.delete()) {
                    JOptionPane.showMessageDialog(this, "File deleted successfully.",
                            "Delete Complete", JOptionPane.INFORMATION_MESSAGE);
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete file.",
                            "Delete Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void openFileLocation(File file) {
        try {
            Desktop.getDesktop().open(file.getParentFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to open file location: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatSize(long size) {
        return org.example.util.FileUtilities.formatFileSize(size);
    }

    // Table model for duplicate pairs
    private class DuplicateTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Group", "Source File", "Source Path", "Master File", "Master Path", "Size", "# Duplicates", "All Duplicate Locations"};
        private List<DuplicatePair> duplicatePairs = new ArrayList<>();
        private java.util.Map<String, Integer> hashToGroupId = new java.util.HashMap<>();
        private java.util.Map<Integer, String> rowToHash = new java.util.HashMap<>();

        public void setDuplicatePairs(List<DuplicatePair> pairs) {
            // Sort pairs by hash to group duplicates together
            this.duplicatePairs = new ArrayList<>(pairs);
            this.duplicatePairs.sort((p1, p2) -> {
                String hash1 = p1.getHash();
                String hash2 = p2.getHash();
                if (hash1 == null && hash2 == null) return 0;
                if (hash1 == null) return 1;
                if (hash2 == null) return -1;
                return hash1.compareTo(hash2);
            });

            // Assign group IDs
            hashToGroupId.clear();
            rowToHash.clear();
            int groupId = 1;
            for (int i = 0; i < this.duplicatePairs.size(); i++) {
                DuplicatePair pair = this.duplicatePairs.get(i);
                String hash = pair.getHash();

                if (!hashToGroupId.containsKey(hash)) {
                    hashToGroupId.put(hash, groupId++);
                }

                rowToHash.put(i, hash);
            }

            fireTableDataChanged();
        }

        public DuplicatePair getDuplicatePairAt(int row) {
            return duplicatePairs.get(row);
        }

        public String getHashAtRow(int row) {
            return rowToHash.get(row);
        }

        public int getGroupIdForRow(int row) {
            String hash = rowToHash.get(row);
            return hash != null ? hashToGroupId.getOrDefault(hash, 0) : 0;
        }

        @Override
        public int getRowCount() {
            return duplicatePairs.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DuplicatePair pair = duplicatePairs.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return String.valueOf(getGroupIdForRow(rowIndex));
                case 1: return pair.getSourceFile().getFileName();
                case 2: return pair.getSourcePath();
                case 3: return pair.getMasterFile().getName();
                case 4: return pair.getMasterPath();
                case 5: return pair.getSourceFile().getFormattedSize();
                case 6:
                    int count = pair.getDuplicateCount();
                    return count > 0 ? String.valueOf(count) : "-";
                case 7:
                    return pair.getFormattedDuplicateLocations();
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }

    // Table model for source duplicate files (grouped by hash)
    private class SourceDuplicateTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Group", "File Name", "Path", "Size", "# in Group"};
        private List<BackupFile> duplicateFiles = new ArrayList<>();
        private java.util.Map<String, Integer> hashToGroupId = new java.util.HashMap<>();
        private java.util.Map<Integer, String> rowToHash = new java.util.HashMap<>();
        private java.util.Map<String, Integer> hashToGroupSize = new java.util.HashMap<>();

        public void setDuplicateGroups(java.util.Map<String, List<BackupFile>> groups) {
            this.duplicateFiles.clear();
            this.hashToGroupId.clear();
            this.rowToHash.clear();
            this.hashToGroupSize.clear();

            // Flatten the groups into a sorted list
            List<java.util.Map.Entry<String, List<BackupFile>>> sortedGroups =
                new ArrayList<>(groups.entrySet());

            // Sort by hash to ensure consistent ordering
            sortedGroups.sort(java.util.Map.Entry.comparingByKey());

            int groupId = 1;
            int rowIndex = 0;

            for (java.util.Map.Entry<String, List<BackupFile>> entry : sortedGroups) {
                String hash = entry.getKey();
                List<BackupFile> filesInGroup = entry.getValue();

                if (filesInGroup.size() > 1) { // Only show actual duplicates (2+ files)
                    hashToGroupId.put(hash, groupId++);
                    hashToGroupSize.put(hash, filesInGroup.size());

                    // Sort files within group by path
                    filesInGroup.sort((f1, f2) -> f1.getPath().compareTo(f2.getPath()));

                    for (BackupFile file : filesInGroup) {
                        duplicateFiles.add(file);
                        rowToHash.put(rowIndex++, hash);
                    }
                }
            }

            fireTableDataChanged();
        }

        public BackupFile getFileAt(int row) {
            if (row >= 0 && row < duplicateFiles.size()) {
                return duplicateFiles.get(row);
            }
            return null;
        }

        public int getGroupIdForRow(int row) {
            String hash = rowToHash.get(row);
            return hash != null ? hashToGroupId.getOrDefault(hash, 0) : 0;
        }

        @Override
        public int getRowCount() {
            return duplicateFiles.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BackupFile file = duplicateFiles.get(rowIndex);
            String hash = rowToHash.get(rowIndex);

            switch (columnIndex) {
                case 0: return String.valueOf(getGroupIdForRow(rowIndex));
                case 1: return file.getFileName();
                case 2: return file.getPath();
                case 3: return file.getFormattedSize();
                case 4: return String.valueOf(hashToGroupSize.getOrDefault(hash, 0));
                default: return null;
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
}
