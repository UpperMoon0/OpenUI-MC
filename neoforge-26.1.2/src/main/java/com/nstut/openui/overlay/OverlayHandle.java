package com.nstut.openui.overlay;

public interface OverlayHandle extends AutoCloseable {
    boolean isOpen();
    @Override void close();
}

