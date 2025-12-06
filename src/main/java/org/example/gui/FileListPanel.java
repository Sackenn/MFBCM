package org.example.gui;

import org.example.model.BackupFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that displays the list of multimedia files found during scanning.
 */
public class FileListPanel extends JPanel {

    private final FileTableModel tableModel;
    private final JTable fileTable;
    private final JLabel summaryLabel;
    private JCheckBox selectAllCheckBox;
    private JComboBox<String> filterComboBox;

    public FileListPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80), 1),
                "Found Files",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(200, 200, 200)
            ),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Create components
        tableModel = new FileTableModel();
        fileTable = new JTable(tableModel);
        fileTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileTable.setRowHeight(28);
        fileTable.setShowGrid(false);
        fileTable.setIntercellSpacing(new Dimension(0, 0));
        setupTable();

        // Create control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // Add table in scroll pane with modern styling
        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 74, 82), 1));
        scrollPane.getViewport().setBackground(new Color(45, 49, 57));
        add(scrollPane, BorderLayout.CENTER);

        // Summary panel with modern styling
        summaryLabel = new JLabel("No files loaded");
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        summaryLabel.setForeground(new Color(180, 180, 180));
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        summaryPanel.setBackground(new Color(40, 44, 52));
        summaryPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(70, 74, 82)));
        summaryPanel.add(summaryLabel);
        add(summaryPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panel.setOpaque(false);

        // Select all checkbox
        selectAllCheckBox = new JCheckBox("Select All");
        selectAllCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selectAllCheckBox.setFocusPainted(false);
        selectAllCheckBox.addActionListener(_ -> {
            boolean selected = selectAllCheckBox.isSelected();
            tableModel.setAllSelected(selected);
            updateSummary();
        });
        panel.add(selectAllCheckBox);

        // Filter dropdown
        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(filterLabel);
        filterComboBox = new JComboBox<>(new String[]{"All", "Images", "Videos", "Selected", "Duplicates"});
        filterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterComboBox.addActionListener(_ -> applyFilter());
        panel.add(filterComboBox);

        return panel;
    }

    private void setupTable() {
        // Set column widths for better proportions
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(40);  // Checkbox
        fileTable.getColumnModel().getColumn(0).setMaxWidth(50);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(250); // Name
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(350); // Path
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // Size
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Date
        fileTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Type
        fileTable.getColumnModel().getColumn(6).setPreferredWidth(100); // Status

        // Custom renderers with dark theme support
        fileTable.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
        fileTable.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor());
        fileTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());

        // Modern table header styling
        fileTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        fileTable.getTableHeader().setBackground(new Color(50, 54, 62));
        fileTable.getTableHeader().setForeground(new Color(200, 200, 200));
        fileTable.getTableHeader().setReorderingAllowed(false);

        // Selection mode
        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileTable.setSelectionBackground(new Color(70, 130, 180));
        fileTable.setSelectionForeground(Color.WHITE);

        // Double-click to toggle selection
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        tableModel.toggleSelection(row);
                        updateSummary();
                    }
                }
            }
        });
    }

    public void setFiles(List<BackupFile> files) {
        tableModel.setFiles(files);
        updateSummary();
        selectAllCheckBox.setSelected(true);
    }

    public void clearFiles() {
        tableModel.clearFiles();
        updateSummary();
    }

    public List<BackupFile> getAllFiles() {
        return tableModel.getAllFiles();
    }

    public List<BackupFile> getSelectedFiles() {
        return tableModel.getSelectedFiles();
    }

    public int getFileCount() {
        return tableModel.getRowCount();
    }

    public void updateFileStatus(BackupFile file) {
        tableModel.updateFileStatus(file);
    }

    private void applyFilter() {
        String filter = (String) filterComboBox.getSelectedItem();
        tableModel.applyFilter(filter != null ? filter : "All");
        updateSummary();
    }

    private void updateSummary() {
        int selectedFiles = tableModel.getSelectedFiles().size();
        int visibleFiles = tableModel.getRowCount();

        long totalSize = tableModel.getAllFiles().stream()
                .mapToLong(BackupFile::getSize)
                .sum();

        long selectedSize = tableModel.getSelectedFiles().stream()
                .mapToLong(BackupFile::getSize)
                .sum();

        String sizeText = formatSize(selectedSize) + " of " + formatSize(totalSize);

        summaryLabel.setText(String.format("Showing %d files | Selected: %d (%s)",
                visibleFiles, selectedFiles, sizeText));
    }

    private String formatSize(long size) {
        return org.example.util.FileUtilities.formatFileSize(size);
    }

    // Custom table model
    private class FileTableModel extends AbstractTableModel {
        private final String[] columnNames = {"", "Name", "Path", "Size", "Date Modified", "Type", "Status"};
        private List<BackupFile> allFiles = new ArrayList<>();
        private final List<BackupFile> filteredFiles = new ArrayList<>();

        @Override
        public int getRowCount() {
            return filteredFiles.size();
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
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) return Boolean.class;
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0; // Only checkbox column is editable
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= filteredFiles.size()) return null;

            BackupFile file = filteredFiles.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> file.isSelected();
                case 1 -> file.getFileName();
                case 2 -> file.getPath();
                case 3 -> file.getFormattedSize();
                case 4 -> file.getFormattedDate();
                case 5 -> file.isVideo() ? "Video" : "Image";
                case 6 -> file.getStatus().toString();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && rowIndex < filteredFiles.size()) {
                BackupFile file = filteredFiles.get(rowIndex);
                file.setSelected((Boolean) value);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        public void setFiles(List<BackupFile> files) {
            this.allFiles = new ArrayList<>(files);
            applyFilter("All");
        }

        public void clearFiles() {
            this.allFiles.clear();
            this.filteredFiles.clear();
            fireTableDataChanged();
        }

        public List<BackupFile> getAllFiles() {
            return new ArrayList<>(allFiles);
        }

        public List<BackupFile> getSelectedFiles() {
            List<BackupFile> selected = new ArrayList<>();
            for (BackupFile file : allFiles) {
                if (file.isSelected()) {
                    selected.add(file);
                }
            }
            return selected;
        }

        public void setAllSelected(boolean selected) {
            for (BackupFile file : allFiles) {
                file.setSelected(selected);
            }
            fireTableDataChanged();
        }

        public void toggleSelection(int row) {
            if (row < filteredFiles.size()) {
                BackupFile file = filteredFiles.get(row);
                file.setSelected(!file.isSelected());
                fireTableCellUpdated(row, 0);
            }
        }

        public void applyFilter(String filter) {
            filteredFiles.clear();

            switch (filter) {
                case "All":
                    filteredFiles.addAll(allFiles);
                    break;
                case "Images":
                    for (BackupFile file : allFiles) {
                        if (file.isImage()) {
                            filteredFiles.add(file);
                        }
                    }
                    break;
                case "Videos":
                    for (BackupFile file : allFiles) {
                        if (file.isVideo()) {
                            filteredFiles.add(file);
                        }
                    }
                    break;
                case "Selected":
                    for (BackupFile file : allFiles) {
                        if (file.isSelected()) {
                            filteredFiles.add(file);
                        }
                    }
                    break;
                case "Duplicates":
                    for (BackupFile file : allFiles) {
                        if (file.getStatus() == BackupFile.BackupStatus.DUPLICATE) {
                            filteredFiles.add(file);
                        }
                    }
                    break;
            }

            fireTableDataChanged();
        }

        public void updateFileStatus(BackupFile file) {
            int index = filteredFiles.indexOf(file);
            if (index >= 0) {
                fireTableRowsUpdated(index, index);
            }
        }
    }

    // Custom checkbox renderer
    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            setSelected(value != null && (Boolean) value);

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            return this;
        }
    }

    // Custom checkbox editor
    private class CheckBoxEditor extends DefaultCellEditor {
        private final JCheckBox checkBox;

        public CheckBoxEditor() {
            super(new JCheckBox());
            this.checkBox = (JCheckBox) getComponent();
            this.checkBox.setHorizontalAlignment(JLabel.CENTER);

            // Create ActionListener once and reuse it
            this.checkBox.addActionListener(_ -> SwingUtilities.invokeLater(FileListPanel.this::updateSummary));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {

            checkBox.setSelected(value != null && (Boolean) value);

            return checkBox;
        }
    }

    // Custom status renderer with dark theme colors
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (value != null) {
                BackupFile.BackupStatus status = BackupFile.BackupStatus.valueOf(value.toString());
                switch (status) {
                    case COMPLETED:
                        setForeground(isSelected ? Color.WHITE : new Color(46, 204, 113));
                        break;
                    case ERROR:
                        setForeground(isSelected ? Color.WHITE : new Color(231, 76, 60));
                        break;
                    case IN_PROGRESS:
                        setForeground(isSelected ? Color.WHITE : new Color(52, 152, 219));
                        break;
                    case DUPLICATE:
                        setForeground(isSelected ? Color.WHITE : new Color(230, 126, 34));
                        break;
                    default:
                        setForeground(isSelected ? Color.WHITE : new Color(200, 200, 200));
                        break;
                }
            }

            return this;
        }
    }
}
