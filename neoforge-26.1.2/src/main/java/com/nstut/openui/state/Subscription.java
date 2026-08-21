package com.nstut.openui.state;

@FunctionalInterface
public interface Subscription extends AutoCloseable {
    Subscription EMPTY = () -> { };

    @Override
    void close();
}

