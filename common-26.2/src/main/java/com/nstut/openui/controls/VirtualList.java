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

public class VirtualList<T> extends UIComponent {
    private final ReadableSignal<List<T>> items;
    private final Function<T, UIComponent> renderer;
    private Function<T, ?> keyExtractor = value -> value;
    private final Map<Object, UIComponent> active = new LinkedHashMap<>();
    private final Map<Object, T> activeItems = new LinkedHashMap<>();
    private Subscription subscription = Subscription.EMPTY;
    private List<T> snapshot = List.of();
    private int itemHeight = 20;
    private int gap;
    private int overscan = 2;
    private double scrollOffset;

    private record IndexedKey(Object value, int index) { }

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
        subscription = items.subscribe(this::updateSnapshot);
        updateSnapshot(items.get());
    }
    @Override protected void onUnmount() { subscription.close(); subscription = Subscription.EMPTY; }
    @Override public int preferredWidth(Font font) { return 100; }
    @Override public int preferredHeight(Font font) { return 80; }

    private void updateSnapshot(List<T> values) {
        snapshot = List.copyOf(values);
        clampScroll();
        invalidateBuild();
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        clampScroll();
        int stride = itemHeight + gap;
        int first = Math.max(0, (int) (scrollOffset / stride) - overscan);
        int last = Math.min(snapshot.size(), (int) Math.ceil((scrollOffset + availableHeight) / stride) + overscan);
        Map<Object, UIComponent> next = new LinkedHashMap<>();
        Map<Object, T> nextItems = new LinkedHashMap<>();
        List<Object> resolvedKeys = new ArrayList<>(last - first);
        for (int index = first; index < last; index++) {
            T item = snapshot.get(index);
            Object raw = keyExtractor.apply(item);
            Object key = raw != null ? raw : index;
            if (next.containsKey(key)) key = new IndexedKey(key, index);
            resolvedKeys.add(key);
            UIComponent component = active.get(key);
            T oldItem = activeItems.get(key);
            if (component != null && !Objects.equals(oldItem, item)) {
                removeChild(component);
                component.dispose();
                component = null;
            }
            if (component == null) component = renderer.apply(item).key(String.valueOf(key));
            next.put(key, component);
            nextItems.put(key, item);
        }
        for (UIComponent child : new ArrayList<>(children)) if (!next.containsValue(child)) { removeChild(child); child.dispose(); }
        for (UIComponent child : next.values()) if (!children.contains(child)) addChild(child);
        active.clear();
        active.putAll(next);
        activeItems.clear();
        activeItems.putAll(nextItems);

        for (int index = first; index < last; index++) {
            UIComponent child = next.get(resolvedKeys.get(index - first));
            if (child == null) continue;
            int childY = y + index * stride - (int) scrollOffset;
            child.layout(x, childY, availableWidth, itemHeight);
        }
    }

    private void clampScroll() {
        double max = Math.max(0, snapshot.size() * (itemHeight + gap) - gap - height);
        scrollOffset = Math.max(0, Math.min(max, scrollOffset));
    }

    @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my,float pt) {
        ClipStack.push(g, x, y, width, height);
        try { renderChildren(g, font, mx, my, pt); } finally { ClipStack.pop(g); }
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < x || mx >= x + width || my < y || my >= y + height) return false;
        if (delta == 0) return false;
        double before = scrollOffset;
        scrollOffset -= delta * (itemHeight + gap) * 0.7;
        clampScroll();
        if (Double.compare(before, scrollOffset) == 0) return false;
        invalidateLayout();
        return true;
    }
}
