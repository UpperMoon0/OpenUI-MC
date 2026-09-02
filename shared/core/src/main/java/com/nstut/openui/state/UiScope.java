package com.nstut.openui.state;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lifecycle-owned resources for one mounted component.
 *
 * <p>A scope is intentionally additive to the existing component lifecycle:
 * legacy components may ignore it, while new components can register effects,
 * subscriptions and other closeable resources without duplicating cleanup in
 * {@code onUnmount()}.</p>
 */
public final class UiScope implements AutoCloseable {
    private final Deque<AutoCloseable> resources = new ArrayDeque<>();
    private final Map<String, Signal<?>> remembered = new HashMap<>();
    private final Map<String, AutoCloseable> keyedResources = new HashMap<>();
    private boolean closed;

    /** Owns a resource and closes it when this scope closes. */
    public <T extends AutoCloseable> T own(T resource) {
        Objects.requireNonNull(resource, "resource");
        ensureOpen();
        resources.push(resource);
        return resource;
    }

    /**
     * Creates one owned resource for the supplied key and reuses it for the
     * lifetime of this mount. This is useful from rerunnable declarative builds.
     */
    @SuppressWarnings("unchecked")
    public <T extends AutoCloseable> T own(String key, Supplier<? extends T> factory) {
        ensureOpen();
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(factory, "factory");
        AutoCloseable existing = keyedResources.get(key);
        if (existing != null) return (T) existing;
        T created = Objects.requireNonNull(factory.get(), "Owned resource factory returned null");
        keyedResources.put(key, created);
        return own(created);
    }

    /** Creates and owns a dependency-tracked effect. */
    public Effect effect(Runnable action) {
        ensureOpen();
        return own(Signals.effect(Objects.requireNonNull(action, "action")));
    }

    /** Subscribes to a signal and owns the returned subscription. */
    public <T> Subscription subscribe(ReadableSignal<T> signal, Consumer<? super T> listener) {
        ensureOpen();
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(listener, "listener");
        return own(signal.subscribe(listener));
    }

    /**
     * Returns mount-scoped remembered state. Repeated calls with the same key
     * return the same signal for the lifetime of this scope.
     */
    @SuppressWarnings("unchecked")
    public <T> Signal<T> remember(String key, Supplier<? extends T> initialValue) {
        ensureOpen();
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(initialValue, "initialValue");
        return (Signal<T>) remembered.computeIfAbsent(key, ignored -> Signals.of(initialValue.get()));
    }

    public <T> Signal<T> remember(String key, T initialValue) {
        return remember(key, () -> initialValue);
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("UI scope is closed");
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        remembered.clear();
        keyedResources.clear();
        RuntimeException failure = null;
        while (!resources.isEmpty()) {
            try {
                resources.pop().close();
            } catch (Exception exception) {
                if (failure == null) failure = new RuntimeException("Failed to close UI scope resource");
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) throw failure;
    }
}
