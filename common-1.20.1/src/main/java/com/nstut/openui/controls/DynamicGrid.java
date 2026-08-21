package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DynamicGrid<T> extends UIComponent {
    private final ReadableSignal<List<T>> items;
    private final Function<T, UIComponent> renderer;
    private Subscription subscription = Subscription.EMPTY;
    private List<T> snapshot;
    private int minCellWidth = 90;
    private int cellHeight = 56;
    private int gap = 6;
    private int lastColumns;

    public DynamicGrid(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) {
        this.items = items; this.renderer = renderer; this.snapshot = List.copyOf(items.get()); rebuild();
    }
    public DynamicGrid<T> minCellWidth(int width) { minCellWidth = Math.max(1, width); invalidateLayout(); return this; }
    public DynamicGrid<T> cellHeight(int height) { cellHeight = Math.max(1, height); invalidateLayout(); return this; }
    public DynamicGrid<T> gap(int gap) { this.gap = Math.max(0, gap); invalidateLayout(); return this; }
    public int columns() { return lastColumns; }
    @Override protected void onMount() { subscription = items.subscribe(values -> { snapshot = List.copyOf(values); rebuild(); }); }
    @Override protected void onUnmount() { subscription.close(); subscription = Subscription.EMPTY; }
    private void rebuild() { clearChildren(); for (T item : snapshot) addChild(renderer.apply(item)); invalidateBuild(); }
    @Override public int preferredWidth(Font font) { return minCellWidth; }
    @Override public int preferredHeight(Font font) { return cellHeight; }
    @Override public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        lastColumns = Math.max(1, (availableWidth + gap) / (minCellWidth + gap));
        int cellWidth = Math.max(1, (availableWidth - gap * (lastColumns - 1)) / lastColumns);
        for (int i = 0; i < children.size(); i++) {
            int row = i / lastColumns, column = i % lastColumns;
            children.get(i).layout(x + column * (cellWidth + gap), y + row * (cellHeight + gap), cellWidth, cellHeight);
        }
    }
    @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) { renderChildren(g, font, mx, my, pt); }
}
