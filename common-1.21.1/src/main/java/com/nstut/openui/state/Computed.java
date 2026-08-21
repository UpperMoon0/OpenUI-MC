package com.nstut.openui.state;

public interface Computed<T> extends ReadableSignal<T>, AutoCloseable {
    @Override
    void close();
}

