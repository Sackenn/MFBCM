package org.example.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Klasa zawierająca stałe i narzędzia do stylizacji interfejsu użytkownika.
 */
public final class UIConstants {

    public static final Color BG_PRIMARY = new Color(40, 44, 52);
    public static final Color BG_SECONDARY = new Color(45, 49, 57);
    public static final Color BG_INPUT = new Color(50, 54, 62);
    public static final Color BORDER_COLOR = new Color(70, 74, 82);
    public static final Color BORDER_LIGHT = new Color(80, 80, 80);

    public static final Color TEXT_PRIMARY = new Color(200, 200, 200);
    public static final Color TEXT_SECONDARY = new Color(180, 180, 180);
    public static final Color TEXT_BRIGHT = new Color(220, 220, 220);

    public static final Color SELECTION_BG = new Color(70, 130, 180);
    public static final Color SELECTION_FG = Color.WHITE;

    public static final Color STATUS_SUCCESS = new Color(46, 204, 113);
    public static final Color STATUS_ERROR = new Color(231, 76, 60);
    public static final Color STATUS_PROGRESS = new Color(52, 152, 219);
    public static final Color STATUS_WARNING = new Color(230, 126, 34);

    public static final Color[] GROUP_COLORS = {
        new Color(55, 59, 67), new Color(60, 64, 72),
        new Color(50, 54, 62), new Color(58, 62, 70),
        new Color(52, 56, 64), new Color(56, 60, 68),
        new Color(54, 58, 66), new Color(57, 61, 69)
    };

    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_LARGE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_LARGE_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_ITALIC = new Font("Segoe UI", Font.ITALIC, 12);

    public static final Dimension BUTTON_SIZE = new Dimension(150, 36);
    public static final Dimension BUTTON_SMALL = new Dimension(120, 32);
    public static final Dimension BUTTON_MEDIUM = new Dimension(180, 32);
    public static final Dimension SPINNER_SIZE = new Dimension(70, 28);
    public static final Dimension LIST_SIZE = new Dimension(400, 60);

    public static final Insets CELL_INSETS = new Insets(8, 8, 8, 8);

    // ====== METODY TWORZĄCE KOMPONENTY ======


    public static JButton createButton(String text) {
        return createButton(text, BUTTON_SIZE);
    }

    public static JButton createButton(String text, Dimension size) {
        JButton button = new JButton(text);
        button.setFont(FONT_BOLD);
        button.setFocusPainted(false);
        button.setPreferredSize(size);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    public static JLabel createLabel(String text) {
        return createLabel(text, FONT_REGULAR, TEXT_PRIMARY);
    }

    public static JLabel createLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static JCheckBox createCheckBox(String text, boolean selected) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setFont(FONT_REGULAR);
        checkBox.setFocusPainted(false);
        return checkBox;
    }

    public static JProgressBar createProgressBar(String initialText) {
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString(initialText);
        progressBar.setFont(FONT_LARGE);
        return progressBar;
    }

    public static Border createTitledBorder(String title) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                title, TitledBorder.LEFT, TitledBorder.TOP, FONT_TITLE, TEXT_PRIMARY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public static Border createStatusBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15));
    }

    public static Border createInputBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    public static <T> void styleList(JList<T> list) {
        list.setFont(FONT_REGULAR);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(BG_INPUT);
        list.setForeground(TEXT_PRIMARY);
    }

    public static JScrollPane createScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setPreferredSize(LIST_SIZE);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        return scrollPane;
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(SELECTION_FG);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(BG_INPUT);
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setReorderingAllowed(false);
    }

    public static JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        return panel;
    }

    public static Color getGroupColor(int groupId) {
        return GROUP_COLORS[(groupId - 1) % GROUP_COLORS.length];
    }

    // Prywatny konstruktor - klasa narzędziowa
    private UIConstants() {
        throw new UnsupportedOperationException("Klasa narzędziowa");
    }
}

