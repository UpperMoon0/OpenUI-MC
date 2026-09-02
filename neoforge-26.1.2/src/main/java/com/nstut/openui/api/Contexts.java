package com.nstut.openui.api;

import com.nstut.openui.context.ContextKey;

import java.util.Optional;

/** Context/provider helpers for retained and declarative components. */
public final class Contexts {
    private Contexts() { }

    public static <T> ContextProvider provide(ContextKey<T> key, T value, UIComponent child) {
        return new ContextProvider(key, value, child);
    }

    public static <T> Optional<T> find(UIComponent component, ContextKey<T> key) {
        for (UIComponent cursor = component; cursor != null; cursor = cursor.parent()) {
            if (cursor instanceof ContextProvider provider) {
                Optional<T> local = provider.findLocal(key);
                if (local.isPresent()) return local;
            }
        }
        return Optional.empty();
    }

    public static <T> T require(UIComponent component, ContextKey<T> key) {
        return find(component, key).orElseThrow(() ->
                new IllegalStateException("Missing OpenUI context value for " + key.debugName()));
    }
}
