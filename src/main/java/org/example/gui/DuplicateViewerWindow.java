package org.example.gui;

import org.example.model.BackupFile;
import org.example.model.DuplicateAnalysisResult;
import org.example.model.DuplicatePair;
import org.example.service.LanguageManager;
import org.example.util.FileUtilities;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.*;
import java.util.List;

import static org.example.gui.UIConstants.*;
import static org.example.service.LanguageManager.get;

/**
 * Okno dialogowe wyświetlające szczegółowy widok wykrytych duplikatów plików.
 */
public class DuplicateViewerWindow extends JDialog {

    private final DuplicateAnalysisResult analysisResult;
    private DuplicateTableModel tableModel;
    private JTable duplicateTable;

    public DuplicateViewerWindow(JFrame parent, DuplicateAnalysisResult analysisResult) {
        super(parent, get("duplicates.title"), true);
        this.analysisResult = analysisResult;
        initializeUI();
        populateData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_BOLD);

        tabbedPane.addTab(get("duplicates.masterDuplicates", analysisResult.getDuplicateInMasterCount()),
            createMasterDuplicatesPanel());
        tabbedPane.addTab(get("duplicates.sourceDuplicates", analysisResult.getDuplicateInSourceCount()),
            createSourceDuplicatesPanel());
        tabbedPane.addTab(get("duplicates.summary"), createSummaryPanel());

        mainContainer.add(tabbedPane, BorderLayout.CENTER);
        add(mainContainer, BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(1000, 650));
        setPreferredSize(new Dimension(1200, 750));
        pack();
        setLocationRelativeTo(getParent());
    }

    // ====== PANELE ZAKŁADEK ======

    private JPanel createMasterDuplicatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(createDescriptionLabel(get("duplicates.masterDescription")), BorderLayout.NORTH);

        tableModel = new DuplicateTableModel();
        duplicateTable = new JTable(tableModel);
        setupDuplicateTable(duplicateTable, tableModel);

        panel.add(createTableScrollPane(duplicateTable), BorderLayout.CENTER);
        panel.add(createMasterActionsPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSourceDuplicatesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(createDescriptionLabel(get("duplicates.sourceDescription")), BorderLayout.NORTH);

        SourceDuplicateTableModel sourceTableModel = new SourceDuplicateTableModel();
        JTable sourceDuplicateTable = new JTable(sourceTableModel);
        setupSourceDuplicateTable(sourceDuplicateTable, sourceTableModel);
        sourceTableModel.setDuplicateGroups(analysisResult.getSourceDuplicateGroups());

        panel.add(createTableScrollPane(sourceDuplicateTable), BorderLayout.CENTER);
        panel.add(createSourceActionsPanel(sourceDuplicateTable, sourceTableModel), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.anchor = GridBagConstraints.WEST;

        Object[][] summaryData = {
            {get("duplicates.masterFiles"), analysisResult.getMasterFileCount(), TEXT_BRIGHT},
            {get("duplicates.sourceFiles"), analysisResult.getTotalSourceFiles(), TEXT_BRIGHT},
            {get("duplicates.uniqueFiles"), analysisResult.getNewFileCount(), STATUS_SUCCESS},
            {get("duplicates.duplicatesInMaster"), analysisResult.getDuplicateInMasterCount(), STATUS_WARNING},
            {get("duplicates.duplicatesInSource"), analysisResult.getDuplicateInSourceCount(), STATUS_WARNING},
            {get("duplicates.totalDuplicates"), analysisResult.getTotalDuplicateCount(), STATUS_ERROR}
        };

        for (int i = 0; i < summaryData.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            Font labelFont = i == 5 ? FONT_LARGE_BOLD : FONT_LARGE;
            panel.add(createLabel((String) summaryData[i][0], labelFont, TEXT_SECONDARY), gbc);

            gbc.gridx = 1;
            Font valueFont = i == 5 ? FONT_LARGE_BOLD : FONT_BOLD;
            panel.add(createLabel(String.valueOf(summaryData[i][1]), valueFont, (Color) summaryData[i][2]), gbc);
        }

        // Potencjalna oszczędność miejsca
        gbc.gridx = 0; gbc.gridy = 6;
        gbc.insets = new Insets(20, 12, 12, 12);
        panel.add(createLabel(get("duplicates.potentialSavings"), FONT_LARGE_BOLD, TEXT_SECONDARY), gbc);

        gbc.gridx = 1;
        long duplicateSize = analysisResult.getDuplicatesInMaster().stream().mapToLong(BackupFile::getSize).sum()
                           + analysisResult.getDuplicatesInSource().stream().mapToLong(BackupFile::getSize).sum();
        panel.add(createLabel(FileUtilities.formatFileSize(duplicateSize), FONT_LARGE_BOLD, STATUS_PROGRESS), gbc);

        return panel;
    }

    // ====== KOMPONENTY POMOCNICZE ======

    private JLabel createDescriptionLabel(String text) {
        JLabel label = new JLabel("<html><i>" + text + "</i></html>");
        label.setFont(FONT_ITALIC);
        label.setForeground(TEXT_SECONDARY);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        return label;
    }

    private JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(BG_SECONDARY);
        return scrollPane;
    }

    private JPanel createMasterActionsPanel() {
        JPanel panel = createActionPanel();

        JButton openSourceBtn = createButton(get("duplicates.openSourceLocation"), BUTTON_MEDIUM);
        openSourceBtn.addActionListener(_ -> openSelectedLocation(true));

        JButton openMasterBtn = createButton(get("duplicates.openMasterLocation"), BUTTON_MEDIUM);
        openMasterBtn.addActionListener(_ -> openSelectedLocation(false));

        JButton deleteBtn = createButton(get("duplicates.deleteSourceFile"), BUTTON_MEDIUM);
        deleteBtn.addActionListener(_ -> deleteSelectedSourceFile());

        panel.add(openSourceBtn);
        panel.add(openMasterBtn);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(deleteBtn);

        return panel;
    }

    private JPanel createSourceActionsPanel(JTable table, SourceDuplicateTableModel model) {
        JPanel panel = createActionPanel();

        JButton openBtn = createButton(get("duplicates.openFileLocation"), BUTTON_MEDIUM);
        openBtn.addActionListener(_ -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                BackupFile file = model.getFileAt(row);
                if (file != null) openFileLocation(file.getSourceFile());
            }
        });

        JButton deleteBtn = createButton(get("duplicates.deleteFile"), BUTTON_MEDIUM);
        deleteBtn.addActionListener(_ -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                BackupFile file = model.getFileAt(row);
                if (file != null) deleteSourceDuplicateFile(file);
            }
        });

        panel.add(openBtn);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(deleteBtn);

        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = createActionPanel();

        JButton refreshBtn = createButton(get("duplicates.refresh"), BUTTON_SMALL);
        refreshBtn.addActionListener(_ -> populateData());

        JButton closeBtn = createButton(get("button.close"), BUTTON_SMALL);
        closeBtn.addActionListener(_ -> dispose());

        panel.add(refreshBtn);
        panel.add(closeBtn);

        return panel;
    }

    // ====== KONFIGURACJA TABEL ======

    private void setupDuplicateTable(JTable table, DuplicateTableModel model) {
        styleTable(table);

        int[] widths = {60, 200, 300, 200, 300, 100};
        table.getColumnModel().getColumn(0).setMaxWidth(80);

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        applyGroupColorRenderer(table, model::getGroupIdForRow, new int[]{0, 5});
        addImagePreviewListeners(table, row -> {
            DuplicatePair pair = model.getDuplicatePairAt(row);
            return pair != null ? pair.getSourceFile() : null;
        });
    }

    private void setupSourceDuplicateTable(JTable table, SourceDuplicateTableModel model) {
        styleTable(table);

        int[] widths = {60, 250, 450, 100, 90};
        table.getColumnModel().getColumn(0).setMaxWidth(80);

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        applyGroupColorRenderer(table, model::getGroupIdForRow, new int[]{0, 4});
        addImagePreviewListeners(table, model::getFileAt);
    }

    private void applyGroupColorRenderer(JTable table, java.util.function.IntFunction<Integer> groupIdGetter, int[] centerColumns) {
        Set<Integer> centerSet = new HashSet<>();
        for (int col : centerColumns) centerSet.add(col);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setFont(FONT_REGULAR);

                if (value != null) {
                    String text = value.toString();
                    setToolTipText(text.length() > 50 ? text : null);
                }

                setHorizontalAlignment(centerSet.contains(column) ? CENTER : LEFT);

                if (!isSelected) {
                    int groupId = groupIdGetter.apply(row);
                    setBackground(getGroupColor(groupId));
                    setForeground(TEXT_PRIMARY);
                }
                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void addImagePreviewListeners(JTable table, java.util.function.IntFunction<BackupFile> fileGetter) {
        table.addMouseMotionListener(new MouseMotionAdapter() {
            private int lastRow = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != lastRow) {
                    lastRow = row;
                    if (row >= 0 && row < table.getRowCount()) {
                        BackupFile file = fileGetter.apply(row);
                        ImagePreviewTooltip.showPreview(file, table, e.getPoint());
                    } else {
                        ImagePreviewTooltip.hidePreview();
                    }
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                ImagePreviewTooltip.hidePreview();
            }
        });
    }

    // ====== AKCJE ======

    private void openSelectedLocation(boolean source) {
        int row = duplicateTable.getSelectedRow();
        if (row >= 0) {
            DuplicatePair pair = tableModel.getDuplicatePairAt(row);
            openFileLocation(source ? pair.getSourceFile().getSourceFile() : pair.getMasterFile());
        }
    }

    private void deleteSelectedSourceFile() {
        int row = duplicateTable.getSelectedRow();
        if (row >= 0) {
            DuplicatePair pair = tableModel.getDuplicatePairAt(row);
            if (confirmDelete(pair.getSourcePath())) {
                if (pair.getSourceFile().getSourceFile().delete()) {
                    JOptionPane.showMessageDialog(this, get("dialog.deleteCompleteMessage", 1),
                        get("dialog.deleteComplete"), JOptionPane.INFORMATION_MESSAGE);
                    populateData();
                } else {
                    showError(get("dialog.deleteError"), get("dialog.error"));
                }
            }
        }
    }

    private void deleteSourceDuplicateFile(BackupFile file) {
        if (confirmDelete(file.getPath())) {
            if (file.getSourceFile().delete()) {
                JOptionPane.showMessageDialog(this, get("dialog.deleteCompleteMessage", 1),
                    get("dialog.deleteComplete"), JOptionPane.INFORMATION_MESSAGE);
                populateData();
            } else {
                showError(get("dialog.deleteError"), get("dialog.error"));
            }
        }
    }

    private boolean confirmDelete(String path) {
        return JOptionPane.showConfirmDialog(this,
            get("dialog.confirmDeleteMessage", 1, path),
            get("dialog.confirmDelete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }


    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void openFileLocation(File file) {
        try {
            Desktop.getDesktop().open(file.getParentFile());
        } catch (Exception e) {
            showError(get("dialog.error") + ": " + e.getMessage(), get("dialog.error"));
        }
    }

    private void populateData() {
        tableModel.setDuplicatePairs(analysisResult.getDuplicatePairs());
    }

    // ====== MODELE TABEL ======

    private static class DuplicateTableModel extends AbstractTableModel {
        private String[] columns = getLocalizedColumns();
        private List<DuplicatePair> duplicatePairs = new ArrayList<>();
        private final Map<String, Integer> hashToGroupId = new HashMap<>();
        private final Map<Integer, String> rowToHash = new HashMap<>();

        private static String[] getLocalizedColumns() {
            return new String[]{
                get("duplicates.columnGroup"),
                get("duplicates.columnSourceFile"),
                get("duplicates.columnSourcePath"),
                get("duplicates.columnMasterFile"),
                get("duplicates.columnMasterPath"),
                get("column.size")
            };
        }

        public void setDuplicatePairs(List<DuplicatePair> pairs) {
            this.duplicatePairs = new ArrayList<>(pairs);
            this.duplicatePairs.sort(Comparator.comparing(DuplicatePair::getHash, Comparator.nullsLast(Comparator.naturalOrder())));

            hashToGroupId.clear();
            rowToHash.clear();
            int groupId = 1;
            for (int i = 0; i < duplicatePairs.size(); i++) {
                String hash = duplicatePairs.get(i).getHash();
                if (!hashToGroupId.containsKey(hash)) {
                    hashToGroupId.put(hash, groupId++);
                }
                rowToHash.put(i, hash);
            }
            fireTableDataChanged();
        }

        public DuplicatePair getDuplicatePairAt(int row) { return duplicatePairs.get(row); }

        public int getGroupIdForRow(int row) {
            String hash = rowToHash.get(row);
            return hash != null ? hashToGroupId.getOrDefault(hash, 0) : 0;
        }

        @Override public int getRowCount() { return duplicatePairs.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) { return String.class; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DuplicatePair pair = duplicatePairs.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> String.valueOf(getGroupIdForRow(rowIndex));
                case 1 -> pair.getSourceFile().getFileName();
                case 2 -> pair.getSourcePath();
                case 3 -> pair.getMasterFile().getName();
                case 4 -> pair.getMasterPath();
                case 5 -> pair.getSourceFile().getFormattedSize();
                default -> null;
            };
        }
    }

    private static class SourceDuplicateTableModel extends AbstractTableModel {
        private String[] columns = getLocalizedColumns();
        private final List<BackupFile> duplicateFiles = new ArrayList<>();
        private final Map<String, Integer> hashToGroupId = new HashMap<>();
        private final Map<Integer, String> rowToHash = new HashMap<>();
        private final Map<String, Integer> hashToGroupSize = new HashMap<>();

        private static String[] getLocalizedColumns() {
            return new String[]{
                get("duplicates.columnGroup"),
                get("column.fileName"),
                get("column.path"),
                get("column.size"),
                get("duplicates.columnFilesInGroup")
            };
        }

        public void setDuplicateGroups(Map<String, List<BackupFile>> groups) {
            duplicateFiles.clear();
            hashToGroupId.clear();
            rowToHash.clear();
            hashToGroupSize.clear();

            List<Map.Entry<String, List<BackupFile>>> sortedGroups = new ArrayList<>(groups.entrySet());
            sortedGroups.sort(Map.Entry.comparingByKey());

            int groupId = 1, rowIndex = 0;
            for (Map.Entry<String, List<BackupFile>> entry : sortedGroups) {
                List<BackupFile> filesInGroup = entry.getValue();
                if (filesInGroup.size() > 1) {
                    String hash = entry.getKey();
                    hashToGroupId.put(hash, groupId++);
                    hashToGroupSize.put(hash, filesInGroup.size());

                    filesInGroup.sort(Comparator.comparing(BackupFile::getPath));
                    for (BackupFile file : filesInGroup) {
                        duplicateFiles.add(file);
                        rowToHash.put(rowIndex++, hash);
                    }
                }
            }
            fireTableDataChanged();
        }

        public BackupFile getFileAt(int row) {
            return row >= 0 && row < duplicateFiles.size() ? duplicateFiles.get(row) : null;
        }

        public int getGroupIdForRow(int row) {
            String hash = rowToHash.get(row);
            return hash != null ? hashToGroupId.getOrDefault(hash, 0) : 0;
        }

        @Override public int getRowCount() { return duplicateFiles.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Class<?> getColumnClass(int columnIndex) { return String.class; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BackupFile file = duplicateFiles.get(rowIndex);
            String hash = rowToHash.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> String.valueOf(getGroupIdForRow(rowIndex));
                case 1 -> file.getFileName();
                case 2 -> file.getPath();
                case 3 -> file.getFormattedSize();
                case 4 -> String.valueOf(hashToGroupSize.getOrDefault(hash, 0));
                default -> null;
            };
        }
    }
}
