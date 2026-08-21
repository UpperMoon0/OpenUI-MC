package com.nstut.openui.runtime;

import com.nstut.openui.api.UIComponent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class FocusManager {
    private UIComponent root;
    private Supplier<List<UIComponent>> overlayRoots = List::of;
    private UIComponent focused;
    private final Deque<UIComponent> focusHistory = new ArrayDeque<>();
    private final Deque<UIComponent> trapRoots = new ArrayDeque<>();

    void setRoot(UIComponent root) {
        this.root = root;
        if (focused != null && !belongsToActiveTree(focused)) {
            setFocusedInternal(null);
        }
    }

    public void setOverlayRoots(Supplier<List<UIComponent>> overlayRoots) {
        this.overlayRoots = overlayRoots != null ? overlayRoots : List::of;
    }

    public UIComponent focused() { return focused; }

    public boolean requestFocus(UIComponent component) {
        if (component == null || !component.isFocusable() || !belongsToActiveTree(component)) return false;
        if (!trapRoots.isEmpty() && !belongsToTree(component, trapRoots.peek())) return false;
        setFocusedInternal(component);
        return true;
    }

    public void clearFocus() { setFocusedInternal(null); }

    public void pushFocus() {
        if (focused != null) {
            focusHistory.push(focused);
        }
    }

    public void restoreFocus() {
        while (!focusHistory.isEmpty()) {
            UIComponent prev = focusHistory.pop();
            if (prev != null && prev.isVisible() && prev.isFocusable() && belongsToActiveTree(prev)) {
                setFocusedInternal(prev);
                return;
            }
        }
        clearFocus();
    }

    public void trapFocus(UIComponent trapRoot) {
        if (trapRoot != null) {
            pushFocus();
            trapRoots.push(trapRoot);
            List<UIComponent> focusable = new ArrayList<>();
            collect(trapRoot, focusable);
            if (!focusable.isEmpty()) {
                setFocusedInternal(focusable.get(0));
            } else {
                setFocusedInternal(null);
            }
        }
    }

    public void untrapFocus(UIComponent trapRoot) {
        if (trapRoots.remove(trapRoot)) {
            restoreFocus();
        }
    }

    public boolean isTrapped() { return !trapRoots.isEmpty(); }
    public UIComponent currentTrapRoot() { return trapRoots.peek(); }

    public boolean focusNext() { return move(1); }
    public boolean focusPrevious() { return move(-1); }

    private boolean move(int direction) {
        List<UIComponent> focusable = new ArrayList<>();
        if (!trapRoots.isEmpty()) {
            collect(trapRoots.peek(), focusable);
        } else {
            collect(root, focusable);
            for (UIComponent overlayRoot : overlayRoots.get()) {
                collect(overlayRoot, focusable);
            }
        }
        if (focusable.isEmpty()) return false;
        int current = focusable.indexOf(focused);
        int next = current < 0
                ? (direction > 0 ? 0 : focusable.size() - 1)
                : Math.floorMod(current + direction, focusable.size());
        setFocusedInternal(focusable.get(next));
        return true;
    }

    private void setFocusedInternal(UIComponent next) {
        if (this.focused == next) return;
        UIComponent prev = this.focused;
        this.focused = next;
        if (prev != null) {
            prev.onFocusLost();
        }
        if (next != null) {
            next.onFocusGained();
        }
    }

    private void collect(UIComponent component, List<UIComponent> output) {
        if (component == null || !component.isVisible()) return;
        if (component.isFocusable()) output.add(component);
        for (UIComponent child : component.children()) collect(child, output);
    }

    public boolean belongsToActiveTree(UIComponent component) {
        if (component == null) return false;
        if (belongsToTree(component, root)) return true;
        for (UIComponent overlay : overlayRoots.get()) {
            if (belongsToTree(component, overlay)) return true;
        }
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
