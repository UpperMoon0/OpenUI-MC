package com.nstut.openui.state;

public interface Signal<T> extends ReadableSignal<T> {
    void set(T value);

    default void update(java.util.function.UnaryOperator<T> updater) {
        set(updater.apply(get()));
    }
}

