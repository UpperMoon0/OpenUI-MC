package com.nstut.openui.controls;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.SelectionModel;
import com.nstut.openui.state.Subscription;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Stable identity key for a retained cell: the logical item key (from keyExtractor)
     * plus the column index. Using the Object itself as part of the record avoids
     * the hashCode-collision problem that a "hash:col" String key would produce.
     */
    private record CellKey(Object itemKey, int column) {}

    private final List<Column<T>> columns = new ArrayList<>();
    private final ReadableSignal<List<T>> itemsSignal;
    private final Map<CellKey, UIComponent> activeCells = new LinkedHashMap<>();
    private final Map<CellKey, Object> cellItems = new LinkedHashMap<>();
    private SelectionModel<T> selectionModel;
    private Subscription subscription = Subscription.EMPTY;
    private int scrollOffset = 0;
    private int hoveredRow = -1;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    private Function<T, Object> keyExtractor = item -> item;

    public Table<T> keyExtractor(Function<T, Object> keyExtractor) {
        this.keyExtractor = Objects.requireNonNull(keyExtractor);
        invalidateLayout();
        return this;
    }

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
    protected void onMount() {
        subscription = itemsSignal.subscribe(ignored -> invalidateLayout());
    }

    @Override
    protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
        for (UIComponent cell : activeCells.values()) {
            removeChild(cell);
            cell.dispose();
        }
        activeCells.clear();
        cellItems.clear();
    }

    public List<T> getSortedItems() {
        List<T> raw = itemsSignal != null ? itemsSignal.get() : List.of();
        List<T> list = new ArrayList<>(raw != null ? raw : List.of());
        if (sortColumn >= 0 && sortColumn < columns.size()) {
            Comparator<T> comp = columns.get(sortColumn).comparator();
            if (comp != null) {
                if (!sortAscending) comp = comp.reversed();
                list.sort(comp);
            }
        }
        return list;
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
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        Font font = measureFont();
        Theme t = theme();
        int headerH = t.tableTheme().headerHeight();
        int rowH = t.tableTheme().rowHeight();

        List<T> items = getSortedItems();
        int bodyH = availableHeight - headerH - 2;
        int visibleRows = Math.max(1, bodyH / rowH);
        int maxScroll = Math.max(0, items.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        int startRow = scrollOffset;
        int endRow = Math.min(items.size(), startRow + visibleRows + 1);

        int[] colWidths = calculateColumnWidths();
        Map<CellKey, UIComponent> nextCells = new LinkedHashMap<>();

        for (int i = startRow; i < endRow; i++) {
            T item = items.get(i);
            int rowY = ly + headerH + 1 + (i - startRow) * rowH;
            if (rowY + rowH > ly + availableHeight - 1) break;

            Object itemKey = keyExtractor != null ? keyExtractor.apply(item) : item;

            int colX = lx + 4;
            for (int c = 0; c < columns.size(); c++) {
                Column<T> col = columns.get(c);
                int cw = colWidths[c];
                // Use a proper record key — avoids hash-collision aliasing from String "hash:col"
                CellKey cellKey = new CellKey(itemKey, c);

                UIComponent cell = activeCells.get(cellKey);
                Object previousItem = cellItems.get(cellKey);

                if (cell != null && previousItem != item) {
                    removeChild(cell);
                    cell.dispose();
                    activeCells.remove(cellKey);
                    cellItems.remove(cellKey);
                    cell = null;
                }

                if (cell == null && col.cellRenderer() != null) {
                    cell = col.cellRenderer().apply(item);
                    cellItems.put(cellKey, item);
                }

                if (cell != null) {
                    nextCells.put(cellKey, cell);
                    if (!children.contains(cell)) {
                        addChild(cell);
                    }
                    cell.layoutTree(font, colX, rowY + (rowH - cell.preferredHeight(font)) / 2, cw - 4, rowH);
                }
                colX += cw;
            }
        }

        // Dispose removed cells
        for (Map.Entry<CellKey, UIComponent> entry : new ArrayList<>(activeCells.entrySet())) {
            if (!nextCells.containsKey(entry.getKey())) {
                removeChild(entry.getValue());
                entry.getValue().dispose();
                cellItems.remove(entry.getKey());
            }
        }
        children.clear();
        for (UIComponent cell : nextCells.values()) {
            if (!children.contains(cell)) {
                children.add(cell);
            }
        }
        activeCells.clear();
        activeCells.putAll(nextCells);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
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
            String sortIndicator = (sortColumn == c) ? (sortAscending ? " ▲" : " ▼") : "";
            Component headerText = Component.empty().append(col.header()).append(sortIndicator);
            g.text(font, headerText, colX, y + (headerH - font.lineHeight) / 2, colors.onSurfaceMuted());
            colX += cw;
        }

        // Rows
        List<T> items = getSortedItems();
        int bodyH = height - headerH - 2;
        int visibleRows = Math.max(1, bodyH / rowH);

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
        }

        // Clip and render retained cell components
        ClipStack.push(g, x + 1, y + headerH + 1, width - 2, height - headerH - 2);
        try {
            for (UIComponent cell : activeCells.values()) {
                cell.render(g, font, mx, my, pt);
            }
        } finally {
            ClipStack.pop(g);
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
            invalidateLayout();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        boolean inBounds = mx >= x && mx < x + width && my >= y && my < y + height;
        if (btn == 0 && inBounds) {
            Theme t = theme();
            int headerH = t.tableTheme().headerHeight();
            int rowH = t.tableTheme().rowHeight();
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
                        invalidateLayout();
                        return true;
                    }
                    colX += colWidths[c];
                }
            } else if (selectionModel != null && my >= y + headerH + 1) {
                int clickedRowIndex = scrollOffset + (int) ((my - (y + headerH + 1)) / rowH);
                List<T> sorted = getSortedItems();
                if (clickedRowIndex >= 0 && clickedRowIndex < sorted.size()) {
                    selectionModel.toggle(sorted.get(clickedRowIndex));
                    invalidatePaint();
                    return true;
                }
            }
        }
        return childrenMouseClicked(mx, my, btn);
    }
}
