package com.nstut.openui.declarative;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Immutable description of one retained child produced by declarative UI code.
 * The factory is used only when reconciliation cannot reuse an existing child;
 * update is applied to both reused and newly-created instances.
 */
public record DeclarativeChild<T>(
        String type,
        String key,
        Supplier<? extends T> factory,
        Consumer<? super T> update) {

    public DeclarativeChild {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");
        update = update == null ? ignored -> { } : update;
    }

    public static <T> DeclarativeChild<T> of(String type, String key, Supplier<? extends T> factory) {
        return new DeclarativeChild<>(type, key, factory, null);
    }

    public NodeIdentity identity() {
        return new NodeIdentity(type, key);
    }

    public T create() {
        T value = Objects.requireNonNull(factory.get(), "Declarative child factory returned null");
        update.accept(value);
        return value;
    }

    public void apply(T value) {
        update.accept(Objects.requireNonNull(value, "value"));
    }
}
