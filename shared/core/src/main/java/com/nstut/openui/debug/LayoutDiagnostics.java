package com.nstut.openui.debug;

import com.nstut.openui.api.PublicApi;
import com.nstut.openui.api.Since;
import com.nstut.openui.api.ScrollGrid;
import com.nstut.openui.api.ScrollList;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.controls.ScrollView;
import com.nstut.openui.layout.Constraints;
import net.minecraft.client.gui.Font;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Public development diagnostics for layout failures that are otherwise visually subtle. */
@PublicApi
@Since("0.0.12")
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
     * Measures natural child height only while diagnostics are enabled, then warns once
     * when it exceeds a bounded visual surface. Explicit scroll containers are excluded
     * before measurement because they already provide accessible overflow.
     */
    public static void checkBoundedOverflow(UIComponent parent, UIComponent child,
                                            int availableWidth, int availableHeight, Font font) {
        if (!enabled() || parent == null || child == null || usesConstraintManagedHeight(child)) return;
        int desiredHeight = child.measure(Constraints.loose(availableWidth, Constraints.INFINITY), font).height();
        // fillHeight() resolves to the supplied maximum during measurement. Under an
        // unbounded diagnostic probe that becomes the sentinel INFINITY rather than
        // evidence that the child actually overflows its bounded layout allocation.
        if (desiredHeight >= Constraints.INFINITY) return;
        warnBoundedOverflow(parent, child, desiredHeight, availableHeight);
    }

    /** Retained for callers that already measured content while diagnostics are active. */
    public static void warnBoundedOverflow(UIComponent parent, UIComponent child,
                                           int desiredHeight, int availableHeight) {
        if (!enabled() || parent == null || child == null || desiredHeight <= availableHeight
                || desiredHeight >= Constraints.INFINITY || usesConstraintManagedHeight(child)) {
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

    /**
     * Conservative diagnostic suppression for subtrees whose vertical size is
     * intentionally resolved by bounded layout rather than intrinsic height. A false
     * negative is preferable to telling a correct flex/scroll layout to add scrolling.
     */
    private static boolean usesConstraintManagedHeight(UIComponent component) {
        if (managesOverflow(component) || component.isFlex()) return true;
        for (UIComponent child : component.children()) {
            if (child.isVisible() && usesConstraintManagedHeight(child)) return true;
        }
        return false;
    }
}
