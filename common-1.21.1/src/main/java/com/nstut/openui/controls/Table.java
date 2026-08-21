package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.SelectionModel;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Table<T> extends UIComponent {
    public record Column<T>(
            Component header,
            int fixedWidth,
            float weight,
            Function<T, UIComponent> cellRenderer,
            Comparator<T> comparator
    ) {
        public Column(String header, int fixedWidth, Function<T, UIComponent> cellRenderer) {
            this(Component.literal(header), fixedWidth, 0.0F, cellRenderer, null);
        }
        public Column(String header, float weight, Function<T, UIComponent> cellRenderer) {
            this(Component.literal(header), 0, weight, cellRenderer, null);
        }
    }

    private final List<Column<T>> columns = new ArrayList<>();
    private final ReadableSignal<List<T>> itemsSignal;
    private SelectionModel<T> selectionModel;
    private int scrollOffset = 0;
    private int hoveredRow = -1;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    public Table(ReadableSignal<List<T>> items) {
        this.itemsSignal = Objects.requireNonNull(items);
        focusable(true);
    }

    public Table<T> column(String header, int fixedWidth, Function<T, UIComponent> cellRenderer) {
        columns.add(new Column<>(header, fixedWidth, cellRenderer));
        invalidateLayout();
        return this;
    }

    public Table<T> column(String header, float weight, Function<T, UIComponent> cellRenderer) {
        columns.add(new Column<>(header, weight, cellRenderer));
        invalidateLayout();
        return this;
    }

    public Table<T> column(Component header, int fixedWidth, float weight, Function<T, UIComponent> cellRenderer, Comparator<T> comparator) {
        columns.add(new Column<>(header, fixedWidth, weight, cellRenderer, comparator));
        invalidateLayout();
        return this;
    }

    public Table<T> selection(SelectionModel<T> selectionModel) {
        this.selectionModel = selectionModel;
        return this;
    }

    @Override
    public int preferredWidth(Font font) {
        int w = 0;
        for (Column<T> col : columns) {
            w += col.fixedWidth() > 0 ? col.fixedWidth() : 80;
        }
        return Math.max(160, w);
    }

    @Override
    public int preferredHeight(Font font) {
        int rowH = theme().tableTheme().rowHeight();
        int headerH = theme().tableTheme().headerHeight();
        return headerH + rowH * 6;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int headerH = t.tableTheme().headerHeight();
        int rowH = t.tableTheme().rowHeight();

        UiRender.roundedOutline(g, x, y, width, height, t.radii().small(), colors.surface(), colors.border());

        // Header
        g.fill(x + 1, y + 1, x + width - 1, y + headerH, colors.surfaceRaised());
        g.fill(x + 1, y + headerH, x + width - 1, y + headerH + 1, colors.borderSubtle());

        int[] colWidths = calculateColumnWidths();
        int colX = x + 4;
        for (int c = 0; c < columns.size(); c++) {
            Column<T> col = columns.get(c);
            int cw = colWidths[c];
            g.drawString(font, col.header(), colX, y + (headerH - font.lineHeight) / 2, colors.onSurfaceMuted());
            colX += cw;
        }

        // Rows
        List<T> rawItems = itemsSignal != null ? itemsSignal.get() : List.of();
        List<T> items = new ArrayList<>(rawItems != null ? rawItems : List.of());

        if (sortColumn >= 0 && sortColumn < columns.size()) {
            Comparator<T> comp = columns.get(sortColumn).comparator();
            if (comp != null) {
                if (!sortAscending) comp = comp.reversed();
                items.sort(comp);
            }
        }

        int bodyH = height - headerH - 2;
        int visibleRows = bodyH / rowH;
        int maxScroll = Math.max(0, items.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int startRow = scrollOffset;
        int endRow = Math.min(items.size(), startRow + visibleRows + 1);

        hoveredRow = -1;
        for (int i = startRow; i < endRow; i++) {
            T item = items.get(i);
            int rowY = y + headerH + 1 + (i - startRow) * rowH;
            if (rowY + rowH > y + height - 1) break;

            boolean isHovered = mx >= x + 2 && mx < x + width - 2 && my >= rowY && my < rowY + rowH;
            if (isHovered) hoveredRow = i;

            boolean isSelected = selectionModel != null && selectionModel.isSelected(item);
            if (isSelected) {
                g.fill(x + 2, rowY, x + width - 2, rowY + rowH, colors.primaryDim());
            } else if (isHovered) {
                g.fill(x + 2, rowY, x + width - 2, rowY + rowH, colors.surfaceVariant());
            } else if (i % 2 == 1) {
                g.fill(x + 2, rowY, x + width - 2, rowY + rowH, colors.surfaceRaised());
            }

            colX = x + 4;
            for (int c = 0; c < columns.size(); c++) {
                Column<T> col = columns.get(c);
                int cw = colWidths[c];
                UIComponent cell = col.cellRenderer().apply(item);
                if (cell != null) {
                    cell.layoutTree(font, colX, rowY + (rowH - cell.preferredHeight(font)) / 2, cw - 4, rowH);
                    cell.render(g, font, mx, my, pt);
                }
                colX += cw;
            }
        }
    }

    private int[] calculateColumnWidths() {
        int[] widths = new int[columns.size()];
        int availableW = width - 8;
        int fixedTotal = 0;
        float totalWeight = 0.0F;

        for (int i = 0; i < columns.size(); i++) {
            Column<T> col = columns.get(i);
            if (col.fixedWidth() > 0) {
                widths[i] = col.fixedWidth();
                fixedTotal += col.fixedWidth();
            } else {
                totalWeight += Math.max(0.1F, col.weight());
            }
        }

        int remainingW = Math.max(0, availableW - fixedTotal);
        for (int i = 0; i < columns.size(); i++) {
            Column<T> col = columns.get(i);
            if (col.fixedWidth() <= 0) {
                float w = Math.max(0.1F, col.weight());
                widths[i] = Math.round(remainingW * (w / totalWeight));
            }
        }
        return widths;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta != 0) {
            scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(delta));
            invalidatePaint();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && isHovered()) {
            Theme t = theme();
            int headerH = t.tableTheme().headerHeight();
            if (my >= y && my < y + headerH) {
                // Header clicked for sorting
                int[] colWidths = calculateColumnWidths();
                int colX = x + 4;
                for (int c = 0; c < columns.size(); c++) {
                    if (mx >= colX && mx < colX + colWidths[c]) {
                        if (sortColumn == c) sortAscending = !sortAscending;
                        else {
                            sortColumn = c;
                            sortAscending = true;
                        }
                        invalidatePaint();
                        return true;
                    }
                    colX += colWidths[c];
                }
            } else if (hoveredRow >= 0 && selectionModel != null) {
                List<T> items = itemsSignal != null ? itemsSignal.get() : List.of();
                if (hoveredRow < items.size()) {
                    selectionModel.toggle(items.get(hoveredRow));
                    invalidatePaint();
                    return true;
                }
            }
        }
        return false;
    }
}
