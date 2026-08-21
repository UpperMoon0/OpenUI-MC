package com.nstut.openui.input;

import com.nstut.openui.api.UIComponent;

public class UiEvent {
    private final EventType type;
    private final UIComponent target;
    private UIComponent currentTarget;
    private EventPhase phase = EventPhase.TARGET;
    private boolean propagationStopped;
    private boolean defaultPrevented;
    /** The component that explicitly requested pointer capture, set immediately at request time. */
    private UIComponent pointerCaptureTarget;

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
    /** True if any component called capturePointer() during dispatch. */
    public boolean isPointerCaptureRequested() { return pointerCaptureTarget != null; }
    /** The component that called capturePointer(), or null if nobody requested it. */
    public UIComponent pointerCaptureTarget() { return pointerCaptureTarget; }

    /**
     * Stops propagation and suppresses OpenUI's legacy/default handler bridge.
     * Use this when neither this event nor ancestor default handlers should run;
     * {@link #preventDefault()} suppresses default handling without stopping listeners.
     */
    public void stopPropagation() { propagationStopped = true; }
    public void preventDefault() { defaultPrevented = true; }

    /**
     * Records the component currently handling this event as the pointer-capture owner.
     * Must be called from within a listener or dispatchEvent() so that currentTarget
     * is still valid. Stores it immediately so that subsequent bubble phases
     * cannot overwrite it.
     */
    public void capturePointer() {
        if (pointerCaptureTarget == null) {
            pointerCaptureTarget = currentTarget;
        }
    }

    public void route(UIComponent currentTarget, EventPhase phase) {
        this.currentTarget = currentTarget;
        this.phase = phase;
    }
}
