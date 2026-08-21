package com.nstut.openui.state;

public interface Effect extends AutoCloseable {
    void run();

    @Override
    void close();
}

