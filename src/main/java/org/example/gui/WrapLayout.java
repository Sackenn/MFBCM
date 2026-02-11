package org.example.gui;

import javax.swing.*;
import java.awt.*;

/**
 * FlowLayout z automatycznym zawijaniem komponentow do nastepnej linii.
 * Uzywany gdy komponenty nie mieszcza sie w jednym wierszu.
 */
public class WrapLayout extends FlowLayout {


    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return calculateLayoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = calculateLayoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension calculateLayoutSize(Container target, boolean usePreferredSize) {
        synchronized (target.getTreeLock()) {
            int maxWidth = calculateMaxWidth(target);
            Dimension dim = calculateComponentsDimension(target, maxWidth, usePreferredSize);
            return adjustForInsets(target, dim);
        }
    }

    private int calculateMaxWidth(Container target) {
        Container container = findParentWithWidth(target);
        int targetWidth = container.getSize().width;
        if (targetWidth == 0) {
            return Integer.MAX_VALUE;
        }
        Insets insets = target.getInsets();
        return targetWidth - insets.left - insets.right - (getHgap() * 2);
    }

    private Container findParentWithWidth(Container target) {
        Container container = target;
        while (container.getSize().width == 0 && container.getParent() != null) {
            container = container.getParent();
        }
        return container;
    }

    private Dimension calculateComponentsDimension(Container target, int maxWidth, boolean usePreferredSize) {
        Dimension dim = new Dimension(0, 0);
        int rowWidth = 0;
        int rowHeight = 0;

        for (int i = 0; i < target.getComponentCount(); i++) {
            Component component = target.getComponent(i);
            if (!component.isVisible()) continue;

            Dimension componentSize = usePreferredSize
                ? component.getPreferredSize()
                : component.getMinimumSize();

            // Jesli komponent nie miesci sie w wierszu, przejdz do nastepnego
            if (rowWidth + componentSize.width > maxWidth && rowWidth > 0) {
                addRowToDimension(dim, rowWidth, rowHeight);
                rowWidth = 0;
                rowHeight = 0;
            }

            // Dodaj szerokosc komponentu do wiersza
            if (rowWidth > 0) {
                rowWidth += getHgap();
            }
            rowWidth += componentSize.width;
            rowHeight = Math.max(rowHeight, componentSize.height);
        }

        addRowToDimension(dim, rowWidth, rowHeight);
        return dim;
    }

    private void addRowToDimension(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) {
            dim.height += getVgap();
        }
        dim.height += rowHeight;
    }

    private Dimension adjustForInsets(Container target, Dimension dim) {
        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right + (getHgap() * 2);
        dim.height += insets.top + insets.bottom + (getVgap() * 2);

        // Przy ukladzie w JScrollPane, dostosuj szerokosc
        if (SwingUtilities.getAncestorOfClass(JScrollPane.class, target) != null && target.isValid()) {
            dim.width -= (getHgap() + 1);
        }
        return dim;
    }
}
