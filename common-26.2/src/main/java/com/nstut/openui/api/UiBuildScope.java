package com.nstut.openui.api;

import com.nstut.openui.context.ContextKey;
import com.nstut.openui.state.AsyncValue;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.ScopedAsync;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.UiScope;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Stable build context passed to rerunnable declarative component builders.
 * State and keyed resources live for exactly one mounted component scope.
 */
public final class UiBuildScope {
    private final UiScope lifecycle;
    private final UIComponent owner;

    UiBuildScope(UiScope lifecycle, UIComponent owner) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /** Advanced access to the mount lifecycle scope. */
    public UiScope lifecycle() { return lifecycle; }

    public <T> Signal<T> remember(String key, T initialValue) {
        return lifecycle.remember(key, initialValue);
    }

    public <T> Signal<T> remember(String key, Supplier<? extends T> initialValue) {
        return lifecycle.remember(key, initialValue);
    }

    public <T> Optional<T> findContext(ContextKey<T> key) {
        return Contexts.find(owner, key);
    }

    public <T> T context(ContextKey<T> key) {
        return Contexts.require(owner, key);
    }

    /**
     * Starts one scope-owned task for this key and exposes declarative
     * loading/success/error state. Change the key to intentionally retry or
     * restart work for a different input.
     */
    public <T> ReadableSignal<AsyncValue<T>> async(
            String key,
            Supplier<? extends T> work,
            Executor background,
            Executor delivery) {
        Objects.requireNonNull(key, "key");
        Signal<AsyncValue<T>> state = lifecycle.remember("async-state:" + key, AsyncValue.loading());
        ScopedAsync.submit(
                lifecycle,
                key,
                work,
                background,
                delivery,
                value -> state.set(AsyncValue.success(value)),
                error -> state.set(AsyncValue.error(error)));
        return state;
    }
}
