package com.nstut.openui.input;

import com.nstut.openui.api.UIComponent;

public class UiEvent {
    private final EventType type;
    private final UIComponent target;
    private UIComponent currentTarget;
    private EventPhase phase = EventPhase.TARGET;
    private boolean propagationStopped;
    private boolean defaultPrevented;
    private boolean pointerCaptureRequested;

    public UiEvent(EventType type, UIComponent target) {
        this.type = type;
        this.target = target;
    }

    public EventType type() { return type; }
    public UIComponent target() { return target; }
    public UIComponent currentTarget() { return currentTarget; }
    public EventPhase phase() { return phase; }
    public boolean isPropagationStopped() { return propagationStopped; }
    public boolean isDefaultPrevented() { return defaultPrevented; }
    public boolean isPointerCaptureRequested() { return pointerCaptureRequested; }
    public void stopPropagation() { propagationStopped = true; }
    public void preventDefault() { defaultPrevented = true; }
    public void capturePointer() { pointerCaptureRequested = true; }

    public void route(UIComponent currentTarget, EventPhase phase) {
        this.currentTarget = currentTarget;
        this.phase = phase;
    }
}

