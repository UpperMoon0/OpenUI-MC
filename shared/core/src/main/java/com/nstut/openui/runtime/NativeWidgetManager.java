package com.nstut.openui.runtime;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

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

    @Override
    public void close() {
        for (AbstractWidget widget : Set.copyOf(registered)) host.remove(widget);
        registered.clear();
    }
}
