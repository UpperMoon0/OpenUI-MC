package com.nstut.openui.debug;

import com.nstut.openui.api.ScrollGrid;
import com.nstut.openui.api.ScrollList;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.controls.ScrollView;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Development-only diagnostics for layout failures that are otherwise visually subtle. */
public final class LayoutDiagnostics {
    private static final boolean PROPERTY_ENABLED = Boolean.getBoolean("openui.debug.layout");
    private static final AtomicInteger DEBUG_SESSIONS = new AtomicInteger();
    private static final Set<String> WARNED_OVERFLOWS = ConcurrentHashMap.newKeySet();

    private LayoutDiagnostics() { }

    /** Enables layout warnings while an OpenUI inspector/debug session is active. */
    public static void openDebugSession() { DEBUG_SESSIONS.incrementAndGet(); }

    /** Disables one inspector/debug session without affecting explicit JVM-property opt in. */
    public static void closeDebugSession() {
        DEBUG_SESSIONS.updateAndGet(value -> Math.max(0, value - 1));
    }

    public static boolean enabled() {
        return PROPERTY_ENABLED || DEBUG_SESSIONS.get() > 0;
    }

    /**
     * Warn once per parent/child instance when natural content height exceeds a bounded
     * visual surface. Explicit scroll containers are intentionally excluded because they
     * already provide accessible overflow.
     */
    public static void warnBoundedOverflow(UIComponent parent, UIComponent child,
                                           int desiredHeight, int availableHeight) {
        if (!enabled() || parent == null || child == null || desiredHeight <= availableHeight || managesOverflow(child)) {
            return;
        }
        String key = System.identityHashCode(parent) + ":" + System.identityHashCode(child);
        if (!WARNED_OVERFLOWS.add(key)) return;
        System.err.println("[OpenUI] Content height exceeds bounded parent; consider Ui.scroll(...). "
                + "parent=" + parent.getClass().getSimpleName()
                + ", child=" + child.getClass().getSimpleName()
                + ", desiredHeight=" + desiredHeight
                + ", availableHeight=" + availableHeight);
    }

    private static boolean managesOverflow(UIComponent child) {
        return child instanceof ScrollView || child instanceof ScrollList || child instanceof ScrollGrid;
    }
}
