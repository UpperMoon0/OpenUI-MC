package com.nstut.openui.overlay;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class OverlayManager implements AutoCloseable {
    private final UiRuntime runtime;
    private final List<Entry> entries = new ArrayList<>();

    public OverlayManager(UiRuntime runtime) { this.runtime = runtime; }

    public OverlayHandle show(OverlayLayer layer, UIComponent component) {
        return show(layer, component, layer == OverlayLayer.MODAL, layer != OverlayLayer.TOOLTIP && layer != OverlayLayer.DEBUG, false, null);
    }

    public OverlayHandle show(OverlayLayer layer, UIComponent component, boolean blocksInput) {
        return show(layer, component, blocksInput, layer != OverlayLayer.TOOLTIP && layer != OverlayLayer.DEBUG, false, null);
    }

    public OverlayHandle show(OverlayLayer layer, UIComponent component, boolean blocksInput,
                              boolean closeOnEscape, boolean closeOnOutsideClick, Runnable onClose) {
        Entry entry = new Entry(layer, Objects.requireNonNull(component), blocksInput, closeOnEscape, closeOnOutsideClick, onClose);
        entries.add(entry);
        entries.sort(Comparator.comparing(Entry::layer));
        component.mount(runtime);
        if (layer == OverlayLayer.MODAL || blocksInput) {
            runtime.focus().trapFocus(component);
        }
        runtime.requestOverlayLayout();
        return entry;
    }

    public boolean closeTopDismissable() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.closeOnEscape && entry.open) {
                entry.close();
                return true;
            }
        }
        return false;
    }

    public void closeTop() {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).close();
    }

    public boolean hasModal() {
        return entries.stream().anyMatch(entry -> entry.layer == OverlayLayer.MODAL && entry.open);
    }

    public boolean hasBlockingOverlay() {
        return entries.stream().anyMatch(entry -> entry.blocksInput && entry.open);
    }

    /** Topmost open overlay that blocks input, or null when none exists. */
    public UIComponent topBlockingComponent() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            Entry entry = entries.get(i);
            if (entry.blocksInput && entry.open) return entry.component;
        }
        return null;
    }

    /** True when the component is an open overlay or lives inside one. */
    public boolean containsComponent(UIComponent component) {
        List<Entry> snapshot = List.copyOf(entries);
        for (Entry entry : snapshot) {
            if (!entry.open) continue;
            for (UIComponent cursor = component; cursor != null; cursor = cursor.parent()) {
                if (cursor == entry.component) return true;
            }
        }
        return false;
    }

    public int size() { return entries.size(); }
    public List<UIComponent> components() { return entries.stream().map(Entry::component).toList(); }

    public void layout(Font font, int x, int y, int width, int height) {
        for (Entry entry : List.copyOf(entries)) {
            entry.component.layoutTree(font, x, y, width, height);
        }
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        for (Entry entry : List.copyOf(entries)) {
            if (entry.layer == OverlayLayer.MODAL && entry.blocksInput) {
                int backdropColor = runtime.theme().colors().backdrop();
                graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), backdropColor);
            }
            entry.component.preRender(mouseX, mouseY);
            entry.component.render(graphics, font, mouseX, mouseY, partialTick);
        }
    }

    public UIComponent hitTest(int mouseX, int mouseY) {
        return hitTest(mouseX, mouseY, false);
    }

    /** Hit-tests a new mouse press and dismisses overlays configured for outside-click closing. */
    public UIComponent hitTestForMouseDown(int mouseX, int mouseY) {
        return hitTest(mouseX, mouseY, true);
    }

    private UIComponent hitTest(int mouseX, int mouseY, boolean dismissOutside) {
        List<Entry> snapshot = List.copyOf(entries);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Entry entry = snapshot.get(i);
            if (!entry.open) continue;
            UIComponent hit = entry.component.hitTest(mouseX, mouseY);
            if (hit != null) return hit;
            if (dismissOutside && entry.closeOnOutsideClick) {
                entry.close();
            }
            if (entry.blocksInput) {
                return entry.component;
            }
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
        private final boolean closeOnEscape;
        private final boolean closeOnOutsideClick;
        private final Runnable onClose;
        private boolean open = true;

        private Entry(OverlayLayer layer, UIComponent component, boolean blocksInput,
                      boolean closeOnEscape, boolean closeOnOutsideClick, Runnable onClose) {
            this.layer = layer;
            this.component = component;
            this.blocksInput = blocksInput;
            this.closeOnEscape = closeOnEscape;
            this.closeOnOutsideClick = closeOnOutsideClick;
            this.onClose = onClose;
        }

        private OverlayLayer layer() { return layer; }
        private UIComponent component() { return component; }
        @Override public boolean isOpen() { return open; }

        @Override public void close() {
            if (!open) return;
            open = false;
            entries.remove(this);
            if (layer == OverlayLayer.MODAL || blocksInput) {
                runtime.focus().untrapFocus(component);
            }
            component.dispose();
            if (onClose != null) onClose.run();
            runtime.requestOverlayLayout();
        }
    }
}
