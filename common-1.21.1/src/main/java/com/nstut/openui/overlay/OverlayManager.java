package com.nstut.openui.overlay;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class OverlayManager implements AutoCloseable {
    private final UiRuntime runtime;
    private final List<Entry> entries = new ArrayList<>();

    public OverlayManager(UiRuntime runtime) { this.runtime = runtime; }

    public OverlayHandle show(OverlayLayer layer, UIComponent component) { return show(layer, component, layer == OverlayLayer.MODAL); }

    public OverlayHandle show(OverlayLayer layer, UIComponent component, boolean blocksInput) {
        Entry entry = new Entry(layer, Objects.requireNonNull(component), blocksInput);
        entries.add(entry);
        entries.sort(Comparator.comparing(Entry::layer));
        component.mount(runtime);
        runtime.requestLayout();
        return entry;
    }

    public void closeTop() {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).close();
    }

    public boolean hasModal() { return entries.stream().anyMatch(entry -> entry.layer == OverlayLayer.MODAL); }
    public int size() { return entries.size(); }
    public List<UIComponent> components() { return entries.stream().map(Entry::component).toList(); }

    public void layout(Font font, int x, int y, int width, int height) {
        for (Entry entry : entries) entry.component.layoutTree(font, x, y, width, height);
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        for (Entry entry : List.copyOf(entries)) {
            entry.component.preRender(mouseX, mouseY);
            entry.component.render(graphics, font, mouseX, mouseY, partialTick);
        }
    }

    public UIComponent hitTest(int mouseX, int mouseY) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            UIComponent hit = entry.component.hitTest(mouseX, mouseY);
            if (hit != null) return hit;
            if (entry.blocksInput) return entry.component;
        }
        return null;
    }

    @Override public void close() {
        for (Entry entry : List.copyOf(entries)) entry.close();
    }

    private final class Entry implements OverlayHandle {
        private final OverlayLayer layer;
        private final UIComponent component;
        private final boolean blocksInput;
        private boolean open = true;

        private Entry(OverlayLayer layer, UIComponent component, boolean blocksInput) {
            this.layer = layer;
            this.component = component;
            this.blocksInput = blocksInput;
        }

        private OverlayLayer layer() { return layer; }
        private UIComponent component() { return component; }
        @Override public boolean isOpen() { return open; }

        @Override public void close() {
            if (!open) return;
            open = false;
            entries.remove(this);
            component.dispose();
            runtime.requestLayout();
        }
    }
}

