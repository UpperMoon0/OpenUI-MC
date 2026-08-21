package com.nstut.openui.animation;

public interface Animation<T> extends AutoCloseable {
    T value();
    boolean isFinished();
    void cancel();
    @Override default void close() { cancel(); }
}

