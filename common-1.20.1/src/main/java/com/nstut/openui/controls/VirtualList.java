package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class VirtualList<T> extends UIComponent {
    private final ReadableSignal<List<T>> items;
    private final Function<T, UIComponent> renderer;
    private Function<T, ?> keyExtractor = value -> value;
    private final Map<Object, UIComponent> active = new LinkedHashMap<>();
    private Subscription subscription = Subscription.EMPTY;
    private List<T> snapshot = List.of();
    private int itemHeight = 20;
    private int gap;
    private int overscan = 2;
    private double scrollOffset;

    public VirtualList(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) {
        this.items = items;
        this.renderer = renderer;
        this.snapshot = List.copyOf(items.get());
    }

    public VirtualList<T> key(Function<T, ?> extractor) { keyExtractor = Objects.requireNonNull(extractor); invalidateBuild(); return this; }
    public VirtualList<T> itemHeight(int height) { itemHeight = Math.max(1, height); invalidateLayout(); return this; }
    public VirtualList<T> gap(int gap) { this.gap = Math.max(0, gap); invalidateLayout(); return this; }
    public VirtualList<T> overscan(int rows) { overscan = Math.max(0, rows); invalidateLayout(); return this; }
    public int activeCellCount() { return active.size(); }

    @Override protected void onMount() {
        subscription = items.subscribe(values -> { snapshot = List.copyOf(values); clampScroll(); invalidateBuild(); });
    }
    @Override protected void onUnmount() { subscription.close(); subscription = Subscription.EMPTY; }
    @Override public int preferredWidth(Font font) { return 100; }
    @Override public int preferredHeight(Font font) { return 80; }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        clampScroll();
        int stride = itemHeight + gap;
        int first = Math.max(0, (int) (scrollOffset / stride) - overscan);
        int last = Math.min(snapshot.size(), (int) Math.ceil((scrollOffset + availableHeight) / stride) + overscan);
        Map<Object, UIComponent> next = new LinkedHashMap<>();
        for (int index = first; index < last; index++) {
            T item = snapshot.get(index);
            Object key = Objects.requireNonNullElse(keyExtractor.apply(item), index);
            UIComponent component = active.get(key);
            if (component == null) component = renderer.apply(item).key(String.valueOf(key));
            next.put(key, component);
        }
        for (UIComponent child : new ArrayList<>(children)) if (!next.containsValue(child)) { removeChild(child); child.dispose(); }
        for (UIComponent child : next.values()) if (!children.contains(child)) addChild(child);
        active.clear();
        active.putAll(next);
        int index = first;
        for (UIComponent child : next.values()) {
            int childY = y + index * stride - (int) scrollOffset;
            child.layout(x, childY, availableWidth, itemHeight);
            index++;
        }
    }

    private void clampScroll() {
        double max = Math.max(0, snapshot.size() * (itemHeight + gap) - gap - height);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset));
    }

    @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        g.enableScissor(x, y, x + width, y + height);
        try { renderChildren(g, font, mx, my, pt); } finally { g.disableScissor(); }
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < x || mx >= x + width || my < y || my >= y + height) return false;
        scrollOffset -= delta * (itemHeight + gap) * 0.7;
        clampScroll();
        invalidateLayout();
        return true;
    }
}

