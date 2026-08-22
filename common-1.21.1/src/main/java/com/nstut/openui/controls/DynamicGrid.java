package com.nstut.openui.controls;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Function;

/**
 * Intrinsically-sized responsive grid. DynamicGrid is deliberately non-scrolling;
 * use VirtualGrid for large/bounded data sets.
 */
public final class DynamicGrid<T> extends UIComponent {
    private final ReadableSignal<List<T>> items;
    private final Function<T, UIComponent> renderer;
    private Subscription subscription = Subscription.EMPTY;
    private List<T> snapshot;
    private int minCellWidth = 90;
    private int cellHeight = 56;
    private int gap = 6;
    private int lastColumns = 1;

    public DynamicGrid(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) {
        this.items = items;
        this.renderer = renderer;
        this.snapshot = List.copyOf(items.get());
        rebuild();
    }

    public DynamicGrid<T> minCellWidth(int width) { minCellWidth = Math.max(1, width); invalidateLayout(); return this; }
    public DynamicGrid<T> cellHeight(int height) { cellHeight = Math.max(1, height); invalidateLayout(); return this; }
    public DynamicGrid<T> gap(int gap) { this.gap = Math.max(0, gap); invalidateLayout(); return this; }
    public int columns() { return lastColumns; }

    @Override protected void onMount() {
        subscription = items.subscribe(values -> { snapshot = List.copyOf(values); rebuild(); });
    }

    @Override protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
    }

    private void rebuild() {
        clearChildren();
        for (T item : snapshot) addChild(renderer.apply(item));
        invalidateBuild();
    }

    private int columnsForWidth(int width) {
        return Math.max(1, (Math.max(1, width) + gap) / (minCellWidth + gap));
    }

    private static int rowCount(int count, int columns) {
        return count <= 0 ? 0 : (count + columns - 1) / columns;
    }

    private int contentHeight(int width) {
        int columns = columnsForWidth(width);
        int rows = rowCount(snapshot.size(), columns);
        return rows == 0 ? 0 : rows * cellHeight + (rows - 1) * gap;
    }

    @Override public int preferredWidth(Font font) { return minCellWidth; }
    @Override public int preferredHeight(Font font) { return contentHeight(minCellWidth); }

    @Override
    public Size measure(Constraints constraints, Font font) {
        int maxWidth = constraints.maxWidth();
        int width = maxWidth >= Constraints.INFINITY
                ? Math.max(constraints.minWidth(), minCellWidth)
                : Math.max(1, maxWidth);
        int desiredHeight = contentHeight(width);
        return constraints.constrain(new Size(width, desiredHeight));
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        lastColumns = columnsForWidth(availableWidth);
        int cellWidth = Math.max(1, (availableWidth - gap * (lastColumns - 1)) / lastColumns);
        for (int i = 0; i < children.size(); i++) {
            int row = i / lastColumns;
            int column = i % lastColumns;
            children.get(i).layout(
                    x + column * (cellWidth + gap),
                    y + row * (cellHeight + gap),
                    cellWidth,
                    cellHeight);
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (width <= 0 || height <= 0) return;
        ClipStack.push(g, x, y, width, height);
        try {
            renderChildren(g, font, mx, my, pt);
        } finally {
            ClipStack.pop(g);
        }
    }
}
