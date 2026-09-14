package com.nstut.openui.context;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable inherited context frame. */
public final class UiContext {
    private static final UiContext EMPTY = new UiContext(null, Map.of());

    private final UiContext parent;
    private final Map<ContextKey<?>, Object> values;

    private UiContext(UiContext parent, Map<ContextKey<?>, Object> values) {
        this.parent = parent;
        this.values = values;
    }

    public static UiContext empty() {
        return EMPTY;
    }

    public <T> UiContext with(ContextKey<T> key, T value) {
        Objects.requireNonNull(key, "key");
        IdentityHashMap<ContextKey<?>, Object> frame = new IdentityHashMap<>();
        frame.put(key, value);
        return new UiContext(this, frame);
    }

    public <T> Optional<T> find(ContextKey<T> key) {
        Objects.requireNonNull(key, "key");
        for (UiContext current = this; current != null; current = current.parent) {
            if (current.values.containsKey(key)) {
                @SuppressWarnings("unchecked")
                T value = (T) current.values.get(key);
                return Optional.ofNullable(value);
            }
        }
        return Optional.empty();
    }

    public <T> T require(ContextKey<T> key) {
        return find(key).orElseThrow(() -> new IllegalStateException(
                "Missing OpenUI context value for " + key.debugName()));
    }
}
