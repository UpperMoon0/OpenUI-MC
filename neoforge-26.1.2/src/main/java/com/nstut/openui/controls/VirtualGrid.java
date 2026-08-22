package com.nstut.openui.controls;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Responsive, vertically scrollable and row-virtualized grid for large signal-backed data sets.
 * Cells are reused by stable key while equal; changed values with the same key are rebuilt so
 * packet-driven rows cannot display stale captured data.
 */
public final class VirtualGrid<T> extends UIComponent {
    private final ReadableSignal<List<T>> items;
    private final Function<T, UIComponent> renderer;
    private Function<T, ?> keyExtractor = value -> value;
    private final Map<Object, UIComponent> active = new LinkedHashMap<>();
    private final Map<Object, T> activeItems = new LinkedHashMap<>();
    private Subscription subscription = Subscription.EMPTY;
    private List<T> snapshot = List.of();
    private int minCellWidth = 90;
    private int cellHeight = 56;
    private int gap = 6;
    private int overscanRows = 1;
    private int lastColumns = 1;
    private double scrollOffset;

    private record IndexedKey(Object value, int index) { }

    public VirtualGrid(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) {
        this.items = Objects.requireNonNull(items);
        this.renderer = Objects.requireNonNull(renderer);
        this.snapshot = List.copyOf(items.get());
    }

    public VirtualGrid<T> key(Function<T, ?> extractor) { keyExtractor = Objects.requireNonNull(extractor); invalidateBuild(); return this; }
    public VirtualGrid<T> minCellWidth(int width) { minCellWidth = Math.max(1, width); invalidateLayout(); return this; }
    public VirtualGrid<T> cellHeight(int height) { cellHeight = Math.max(1, height); invalidateLayout(); return this; }
    public VirtualGrid<T> gap(int gap) { this.gap = Math.max(0, gap); invalidateLayout(); return this; }
    public VirtualGrid<T> overscanRows(int rows) { overscanRows = Math.max(0, rows); invalidateLayout(); return this; }
    public int columns() { return lastColumns; }
    public int activeCellCount() { return active.size(); }
    public double scrollOffset() { return scrollOffset; }
    public void resetScroll() { if (scrollOffset != 0) { scrollOffset = 0; invalidateLayout(); } }

    @Override protected void onMount() {
        subscription = items.subscribe(values -> {
            snapshot = List.copyOf(values);
            clampScroll();
            invalidateBuild();
        });
    }

    @Override protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
    }

    @Override public int preferredWidth(Font font) { return minCellWidth; }
    @Override public int preferredHeight(Font font) { return Math.max(cellHeight, 80); }

    private int columnsForWidth(int width) {
        return Math.max(1, (Math.max(1, width) + gap) / (minCellWidth + gap));
    }

    private int totalRows() {
        return snapshot.isEmpty() ? 0 : (snapshot.size() + lastColumns - 1) / lastColumns;
    }

    private int contentHeight() {
        int rows = totalRows();
        return rows == 0 ? 0 : rows * cellHeight + (rows - 1) * gap;
    }

    private void clampScroll() {
        double max = Math.max(0, contentHeight() - height);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset));
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        lastColumns = columnsForWidth(availableWidth);
        clampScroll();

        int rows = totalRows();
        int stride = cellHeight + gap;
        int firstRow = Math.max(0, (int) (scrollOffset / stride) - overscanRows);
        int lastRow = Math.min(rows,
                (int) Math.ceil((scrollOffset + availableHeight) / (double) stride) + overscanRows);
        int firstIndex = Math.min(snapshot.size(), firstRow * lastColumns);
        int lastIndex = Math.min(snapshot.size(), lastRow * lastColumns);

        Map<Object, UIComponent> next = new LinkedHashMap<>();
        Map<Object, T> nextItems = new LinkedHashMap<>();

        for (int index = firstIndex; index < lastIndex; index++) {
            T item = snapshot.get(index);
            Object rawKey = keyExtractor.apply(item);
            Object key = rawKey != null ? rawKey : index;
            if (next.containsKey(key)) key = new IndexedKey(key, index);

            UIComponent component = active.get(key);
            T previousItem = activeItems.get(key);
            if (component != null && !Objects.equals(previousItem, item)) {
                removeChild(component);
                component.dispose();
                component = null;
            }
            if (component == null) component = renderer.apply(item).key(String.valueOf(key));
            next.put(key, component);
            nextItems.put(key, item);
        }

        for (UIComponent child : new ArrayList<>(children)) {
            if (!next.containsValue(child)) {
                removeChild(child);
                child.dispose();
            }
        }
        for (UIComponent child : next.values()) if (!children.contains(child)) addChild(child);

        active.clear();
        active.putAll(next);
        activeItems.clear();
        activeItems.putAll(nextItems);

        int cellWidth = Math.max(1, (availableWidth - gap * (lastColumns - 1)) / lastColumns);
        for (int index = firstIndex; index < lastIndex; index++) {
            T item = snapshot.get(index);
            Object rawKey = keyExtractor.apply(item);
            Object key = rawKey != null ? rawKey : index;
            UIComponent child = next.get(key);
            if (child == null) {
                for (Map.Entry<Object, UIComponent> e : next.entrySet()) {
                    if (e.getKey() instanceof IndexedKey ik && ik.index() == index) { child = e.getValue(); break; }
                }
            }
            if (child == null) continue;
            int row = index / lastColumns;
            int column = index % lastColumns;
            int childX = x + column * (cellWidth + gap);
            int childY = y + row * stride - (int) scrollOffset;
            child.layout(childX, childY, cellWidth, cellHeight);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (width <= 0 || height <= 0) return;
        ClipStack.push(g, x, y, width, height);
        try {
            renderChildren(g, font, mx, my, pt);
        } finally {
            ClipStack.pop(g);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        active.clear();
        activeItems.clear();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < x || mx >= x + width || my < y || my >= y + height || delta == 0) return false;
        scrollOffset -= delta * (cellHeight + gap) * 0.7;
        clampScroll();
        invalidateLayout();
        return true;
    }
}
