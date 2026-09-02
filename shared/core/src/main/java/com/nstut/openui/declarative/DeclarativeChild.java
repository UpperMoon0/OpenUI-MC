package com.nstut.openui.declarative;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Immutable description of one retained node produced by declarative UI code.
 *
 * <p>The factory is used only when reconciliation cannot reuse an existing
 * retained node. The update callback is applied to both new and reused nodes.
 * Child descriptions opt the node into declarative ownership of its direct
 * children; a description with no children remains a leaf and may wrap a
 * legacy retained component that manages its own internal subtree.</p>
 */
public record DeclarativeChild<T>(
        String type,
        String key,
        Supplier<? extends T> factory,
        Consumer<? super T> update,
        List<DeclarativeChild<T>> children) {

    public DeclarativeChild {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");
        update = update == null ? ignored -> { } : update;
        children = children == null ? List.of() : List.copyOf(children);
    }

    /** Backward-compatible leaf-description constructor. */
    public DeclarativeChild(String type, String key, Supplier<? extends T> factory, Consumer<? super T> update) {
        this(type, key, factory, update, List.of());
    }

    public static <T> DeclarativeChild<T> of(String type, String key, Supplier<? extends T> factory) {
        return new DeclarativeChild<>(type, key, factory, null, List.of());
    }

    public static <T> DeclarativeChild<T> of(
            String type,
            String key,
            Supplier<? extends T> factory,
            List<DeclarativeChild<T>> children) {
        return new DeclarativeChild<>(type, key, factory, null, children);
    }

    public DeclarativeChild<T> withUpdate(Consumer<? super T> nextUpdate) {
        return new DeclarativeChild<>(type, key, factory, nextUpdate, children);
    }

    public DeclarativeChild<T> withChildren(List<DeclarativeChild<T>> nextChildren) {
        return new DeclarativeChild<>(type, key, factory, update, nextChildren);
    }

    @SafeVarargs
    public final DeclarativeChild<T> withChildren(DeclarativeChild<T>... nextChildren) {
        return withChildren(List.of(nextChildren));
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
