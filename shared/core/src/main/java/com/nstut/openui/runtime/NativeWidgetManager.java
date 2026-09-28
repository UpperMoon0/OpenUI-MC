package com.nstut.openui.runtime;

import com.nstut.openui.api.Internal;
import com.nstut.openui.api.Since;
import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class NativeWidgetManager implements AutoCloseable {
    private final NativeWidgetHost host;
    private final Set<AbstractWidget> registered = Collections.newSetFromMap(new IdentityHashMap<>());

    public NativeWidgetManager(NativeWidgetHost host) { this.host = host; }

    public void synchronize(UIComponent root) {
        synchronize(root, java.util.List.of());
    }

    public void synchronize(UIComponent root, Iterable<UIComponent> additionalRoots) {
        Set<AbstractWidget> current = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(root, current);
        for (UIComponent additionalRoot : additionalRoots) collect(additionalRoot, current);
        for (AbstractWidget widget : Set.copyOf(registered)) {
            if (!current.contains(widget)) {
                host.remove(widget);
                registered.remove(widget);
            }
        }
        for (AbstractWidget widget : current) {
            if (registered.add(widget)) host.add(widget);
        }
    }

    private void collect(UIComponent component, Set<AbstractWidget> widgets) {
        if (component instanceof NativeWidgetOwner owner) widgets.add(owner.nativeWidget());
        for (UIComponent child : component.children()) collect(child, widgets);
    }

    /**
     * Runs vanilla Screen mouse-click fallback with OpenUI-owned native widgets temporarily inactive.
     * OpenUI gets first chance to route the click through clip-aware component hit-testing; if it declines,
     * vanilla may still handle unrelated screen children, but it cannot dispatch the same click directly
     * to an EditBox/native widget and bypass an ancestor OpenUI clip.
     */
    @Internal
    @Since("0.0.12")
    public boolean withMouseClickFallbackSuppressed(BooleanSupplier fallback) {
        Map<AbstractWidget, Boolean> previousActive = new IdentityHashMap<>();
        for (AbstractWidget widget : registered) {
            previousActive.put(widget, widget.active);
            widget.active = false;
        }
        try {
            return fallback.getAsBoolean();
        } finally {
            for (Map.Entry<AbstractWidget, Boolean> entry : previousActive.entrySet()) {
                entry.getKey().active = entry.getValue();
            }
        }
    }

    @Override
    public void close() {
        for (AbstractWidget widget : Set.copyOf(registered)) host.remove(widget);
        registered.clear();
    }
}
