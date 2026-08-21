package com.nstut.openui.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class StateStore implements AutoCloseable {
    private final Map<String, Signal<?>> values = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Signal<T> remember(String key, Supplier<T> initialValue) {
        return (Signal<T>) values.computeIfAbsent(key, ignored -> Signals.of(initialValue.get()));
    }
    public <T> Signal<T> remember(String key, T initialValue) { return remember(key, () -> initialValue); }
    public void forget(String key) { values.remove(key); }
    @Override public void close() { values.clear(); }
}
