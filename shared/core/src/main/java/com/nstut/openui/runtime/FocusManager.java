package com.nstut.openui.runtime;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.input.SpatialNavigation;
import com.nstut.openui.semantics.SemanticNarration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public final class FocusManager {
    private record FocusTrap(UIComponent trapRoot, UIComponent previousFocus) {}

    private UIComponent root;
    private Supplier<List<UIComponent>> overlayRoots = List::of;
    private UIComponent focused;
    private final Deque<UIComponent> focusHistory = new ArrayDeque<>();
    private final Deque<FocusTrap> traps = new ArrayDeque<>();

    void setRoot(UIComponent root) {
        this.root = root;
        if (focused != null && !belongsToActiveTree(focused)) setFocusedInternal(null);
    }

    public void setOverlayRoots(Supplier<List<UIComponent>> overlayRoots) {
        this.overlayRoots = overlayRoots != null ? overlayRoots : List::of;
    }

    public UIComponent focused() { return focused; }

    /** Narration for the nearest semantic ancestor of the current focus target. */
    public String focusedNarration() { return SemanticNarration.describeNearest(focused); }

    public boolean requestFocus(UIComponent component) {
        if (component == null || !component.isFocusable() || !belongsToActiveTree(component)) return false;
        FocusTrap top = traps.peek();
        if (top != null && !belongsToTree(component, top.trapRoot())) return false;
        setFocusedInternal(component);
        return true;
    }

    public void clearFocus() { setFocusedInternal(null); }

    /** Compatibility API retained from 0.0.7: remembers the current focus for later restoration. */
    public void pushFocus() {
        if (focused != null) focusHistory.push(focused);
    }

    /** Compatibility API retained from 0.0.7 with its original restoration semantics. */
    public void restoreFocus() {
        while (!focusHistory.isEmpty()) {
            UIComponent previous = focusHistory.pop();
            if (previous.isVisible() && previous.isFocusable() && belongsToActiveTree(previous)) {
                setFocusedInternal(previous);
                return;
            }
        }
        clearFocus();
    }

    public void trapFocus(UIComponent trapRoot) {
        if (trapRoot == null) return;
        traps.push(new FocusTrap(trapRoot, focused));
        List<UIComponent> focusable = new ArrayList<>();
        collect(trapRoot, focusable);
        setFocusedInternal(focusable.isEmpty() ? null : focusable.get(0));
    }

    public void untrapFocus(UIComponent trapRoot) {
        FocusTrap top = traps.peek();
        if (top == null || top.trapRoot() != trapRoot) {
            FocusTrap removed = removeTrap(trapRoot);
            if (removed != null) rewirePreviousFocus(removed);
            return;
        }

        traps.pop();
        FocusTrap parent = traps.peek();
        UIComponent restored = validRestoreTarget(top.previousFocus(), parent) ? top.previousFocus() : null;
        if (restored == null && parent != null) {
            List<UIComponent> focusable = new ArrayList<>();
            collect(parent.trapRoot(), focusable);
            if (!focusable.isEmpty()) restored = focusable.get(0);
        }
        setFocusedInternal(restored);
    }

    private boolean validRestoreTarget(UIComponent component, FocusTrap parent) {
        return component != null
                && component.isVisible()
                && component.isFocusable()
                && belongsToActiveTree(component)
                && (parent == null || belongsToTree(component, parent.trapRoot()));
    }

    private FocusTrap removeTrap(UIComponent trapRoot) {
        for (Iterator<FocusTrap> it = traps.iterator(); it.hasNext(); ) {
            FocusTrap trap = it.next();
            if (trap.trapRoot() == trapRoot) {
                it.remove();
                return trap;
            }
        }
        return null;
    }

    private void rewirePreviousFocus(FocusTrap removed) {
        List<FocusTrap> frames = new ArrayList<>(traps);
        for (int i = 0; i < frames.size(); i++) {
            FocusTrap trap = frames.get(i);
            if (belongsToTree(trap.previousFocus(), removed.trapRoot())) {
                frames.set(i, new FocusTrap(trap.trapRoot(), removed.previousFocus()));
            }
        }
        traps.clear();
        for (int i = frames.size() - 1; i >= 0; i--) traps.push(frames.get(i));
    }

    public boolean isTrapped() { return !traps.isEmpty(); }
    public UIComponent currentTrapRoot() {
        FocusTrap top = traps.peek();
        return top != null ? top.trapRoot() : null;
    }

    public boolean focusNext() { return move(1); }
    public boolean focusPrevious() { return move(-1); }

    /** Moves focus spatially, suitable for arrow keys and controllers. */
    public boolean focusDirection(SpatialNavigation.Direction direction) {
        List<UIComponent> focusable = activeFocusable();
        if (focusable.isEmpty()) return false;
        UIComponent current = focused;
        if (current == null || !focusable.contains(current)) {
            setFocusedInternal(focusable.get(0));
            return true;
        }
        SpatialNavigation.Target<UIComponent> currentTarget = target(current);
        List<SpatialNavigation.Target<UIComponent>> targets = focusable.stream().map(FocusManager::target).toList();
        return SpatialNavigation.next(currentTarget, targets, direction).map(this::requestFocus).orElse(false);
    }

    private boolean move(int direction) {
        List<UIComponent> focusable = activeFocusable();
        if (focusable.isEmpty()) return false;
        int current = focusable.indexOf(focused);
        int next = current < 0
                ? (direction > 0 ? 0 : focusable.size() - 1)
                : Math.floorMod(current + direction, focusable.size());
        setFocusedInternal(focusable.get(next));
        return true;
    }

    private List<UIComponent> activeFocusable() {
        List<UIComponent> focusable = new ArrayList<>();
        FocusTrap top = traps.peek();
        if (top != null) collect(top.trapRoot(), focusable);
        else {
            collect(root, focusable);
            for (UIComponent overlayRoot : overlayRoots.get()) collect(overlayRoot, focusable);
        }
        return focusable;
    }

    private static SpatialNavigation.Target<UIComponent> target(UIComponent component) {
        return new SpatialNavigation.Target<>(component, component.getX(), component.getY(), component.getWidth(), component.getHeight());
    }

    private void setFocusedInternal(UIComponent next) {
        if (this.focused == next) return;
        UIComponent prev = this.focused;
        this.focused = next;
        if (prev != null) prev.onFocusLost();
        if (next != null) next.onFocusGained();
    }

    private void collect(UIComponent component, List<UIComponent> output) {
        if (component == null || !component.isVisible()) return;
        if (component.isFocusable()) output.add(component);
        for (UIComponent child : component.children()) collect(child, output);
    }

    public boolean belongsToActiveTree(UIComponent component) {
        if (component == null) return false;
        if (belongsToTree(component, root)) return true;
        for (UIComponent overlay : overlayRoots.get()) if (belongsToTree(component, overlay)) return true;
        return false;
    }

    private static boolean belongsToTree(UIComponent component, UIComponent targetRoot) {
        if (targetRoot == null) return false;
        UIComponent cursor = component;
        while (cursor != null) {
            if (cursor == targetRoot) return true;
            cursor = cursor.parent();
        }
        return false;
    }
}
