package com.nstut.openui.input;

import com.nstut.openui.api.UIComponent;

public final class PointerEvent extends UiEvent {
    private final double x, y, deltaX, deltaY;
    private final int button;

    public PointerEvent(EventType type, UIComponent target, double x, double y, int button, double deltaX, double deltaY) {
        super(type, target);
        this.x = x;
        this.y = y;
        this.button = button;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public double x() { return x; }
    public double y() { return y; }
    public int button() { return button; }
    public double deltaX() { return deltaX; }
    public double deltaY() { return deltaY; }
}

