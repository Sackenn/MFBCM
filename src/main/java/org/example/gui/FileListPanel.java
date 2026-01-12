package org.example.gui;

import org.example.model.BackupFile;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import static org.example.gui.UIConstants.*;


/**
 * Panel wyświetlający listę plików do kopii zapasowej w formie tabeli.
 */
public class FileListPanel extends JPanel {

    private final FileTableModel tableModel;
    private final JTable fileTable;
    private final JLabel summaryLabel;
    private JCheckBox selectAllCheckBox;
    private JComboBox<String> filterComboBox;
    private Runnable selectionChangeListener;

    public FileListPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(createTitledBorder("Found Files"));

        tableModel = new FileTableModel();
        fileTable = new JTable(tableModel);
        setupTable();

        add(createControlPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(BG_SECONDARY);
        add(scrollPane, BorderLayout.CENTER);

        summaryLabel = createLabel("No files loaded", FONT_REGULAR, TEXT_SECONDARY);
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        summaryPanel.setBackground(BG_PRIMARY);
        summaryPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        summaryPanel.add(summaryLabel);
        add(summaryPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        panel.setOpaque(false);

        selectAllCheckBox = createCheckBox("Select All", false);
        selectAllCheckBox.addActionListener(_ -> {
            tableModel.setAllSelected(selectAllCheckBox.isSelected());
            updateSummary();
        });
        panel.add(selectAllCheckBox);

        panel.add(createLabel("Filter:"));
        filterComboBox = new JComboBox<>(new String[]{"All", "Images", "Videos", "Selected", "Duplicates"});
        filterComboBox.setFont(FONT_REGULAR);
        filterComboBox.addActionListener(_ -> applyFilter());
        panel.add(filterComboBox);

        return panel;
    }

    private void setupTable() {
        styleTable(fileTable);
        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Szerokości kolumn
        int[] widths = {40, 250, 350, 90, 140, 80, 100};
        int[] maxWidths = {50, -1, -1, -1, -1, -1, -1};

        for (int i = 0; i < widths.length; i++) {
            fileTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (maxWidths[i] > 0) {
                fileTable.getColumnModel().getColumn(i).setMaxWidth(maxWidths[i]);
            }
        }

        // Renderery
        fileTable.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxRenderer());
        fileTable.getColumnModel().getColumn(0).setCellEditor(new CheckBoxEditor());
        fileTable.getColumnModel().getColumn(6).setCellRenderer(new StatusRenderer());

        // Podwójne kliknięcie przełącza zaznaczenie
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

        // Podgląd obrazu przy najechaniu myszką
        fileTable.addMouseMotionListener(new MouseMotionAdapter() {
            private int lastRow = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = fileTable.rowAtPoint(e.getPoint());
                if (row != lastRow) {
                    lastRow = row;
                    if (row >= 0 && row < tableModel.getRowCount()) {
                        BackupFile file = tableModel.getFileAt(row);
                        ImagePreviewTooltip.showPreview(file, fileTable, e.getPoint());
                    } else {
                        ImagePreviewTooltip.hidePreview();
                    }
                }
            }
        });

        // Ukryj podgląd gdy mysz opuści tabelę
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                ImagePreviewTooltip.hidePreview();
            }
        });
    }

    // ====== PUBLICZNE API ======

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

    public void setSelectionChangeListener(Runnable listener) {
        this.selectionChangeListener = listener;
    }

    private void notifySelectionChanged() {
        if (selectionChangeListener != null) {
            selectionChangeListener.run();
        }
    }

    // ====== METODY PRYWATNE ======

    private void applyFilter() {
        String filter = (String) filterComboBox.getSelectedItem();
        tableModel.applyFilter(filter != null ? filter : "All");
        updateSummary();
    }

    public void updateSummary() {
        int selectedFiles = tableModel.getSelectedFiles().size();
        int visibleFiles = tableModel.getRowCount();

        long totalSize = tableModel.getAllFiles().stream().mapToLong(BackupFile::getSize).sum();
        long selectedSize = tableModel.getSelectedFiles().stream().mapToLong(BackupFile::getSize).sum();

        String sizeText = formatSize(selectedSize) + " of " + formatSize(totalSize);
        summaryLabel.setText(String.format("Showing %d files | Selected: %d (%s)", visibleFiles, selectedFiles, sizeText));

        notifySelectionChanged();
    }

    private String formatSize(long size) {
        return org.example.util.FileUtilities.formatFileSize(size);
    }

    // ====== RENDERERY ======

    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected(value != null && (Boolean) value);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
            return this;
        }
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(FONT_REGULAR);

            if (value != null) {
                Color statusColor = switch (BackupFile.BackupStatus.valueOf(value.toString())) {
                    case COMPLETED -> STATUS_SUCCESS;
                    case ERROR -> STATUS_ERROR;
                    case IN_PROGRESS -> STATUS_PROGRESS;
                    case DUPLICATE -> STATUS_WARNING;
                    default -> TEXT_PRIMARY;
                };
                setForeground(isSelected ? Color.WHITE : statusColor);
            }
            return this;
        }
    }

    // ====== MODEL TABELI ======

    private static class FileTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"", "Name", "Path", "Size", "Date Modified", "Type", "Status"};
        private final List<BackupFile> filteredFiles = new ArrayList<>();
        private List<BackupFile> allFiles = new ArrayList<>();

        @Override
        public int getRowCount() { return filteredFiles.size(); }

        @Override
        public int getColumnCount() { return COLUMN_NAMES.length; }

        @Override
        public String getColumnName(int column) { return COLUMN_NAMES[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
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
                filteredFiles.get(rowIndex).setSelected((Boolean) value);
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

        public List<BackupFile> getAllFiles() { return new ArrayList<>(allFiles); }

        public BackupFile getFileAt(int row) {
            return row >= 0 && row < filteredFiles.size() ? filteredFiles.get(row) : null;
        }

        public List<BackupFile> getSelectedFiles() {
            return allFiles.stream().filter(BackupFile::isSelected).toList();
        }

        public void setAllSelected(boolean selected) {
            allFiles.forEach(f -> f.setSelected(selected));
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
            filteredFiles.addAll(switch (filter) {
                case "Images" -> allFiles.stream().filter(BackupFile::isImage).toList();
                case "Videos" -> allFiles.stream().filter(BackupFile::isVideo).toList();
                case "Selected" -> allFiles.stream().filter(BackupFile::isSelected).toList();
                case "Duplicates" -> allFiles.stream()
                    .filter(f -> f.getStatus() == BackupFile.BackupStatus.DUPLICATE).toList();
                default -> allFiles;
            });
            fireTableDataChanged();
        }

        public void updateFileStatus(BackupFile file) {
            int index = filteredFiles.indexOf(file);
            if (index >= 0) {
                fireTableRowsUpdated(index, index);
            }
        }
    }

    // ====== EDYTOR CHECKBOX ======

    private class CheckBoxEditor extends DefaultCellEditor {
        private final JCheckBox checkBox;

        public CheckBoxEditor() {
            super(new JCheckBox());
            this.checkBox = (JCheckBox) getComponent();
            this.checkBox.setHorizontalAlignment(JLabel.CENTER);
            this.checkBox.addActionListener(_ -> SwingUtilities.invokeLater(FileListPanel.this::updateSummary));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            checkBox.setSelected(value != null && (Boolean) value);
            return checkBox;
        }
    }
}
