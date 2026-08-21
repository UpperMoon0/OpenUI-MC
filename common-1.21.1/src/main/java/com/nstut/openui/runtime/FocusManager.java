package com.nstut.openui.runtime;

import com.nstut.openui.api.UIComponent;

import java.util.ArrayList;
import java.util.List;

public final class FocusManager {
    private UIComponent root;
    private UIComponent focused;

    void setRoot(UIComponent root) {
        this.root = root;
        if (focused != null && !belongsToRoot(focused)) focused = null;
    }

    public UIComponent focused() { return focused; }

    public boolean requestFocus(UIComponent component) {
        if (component == null || !component.isFocusable() || !belongsToRoot(component)) return false;
        focused = component;
        return true;
    }

    public void clearFocus() { focused = null; }

    public boolean focusNext() { return move(1); }
    public boolean focusPrevious() { return move(-1); }

    private boolean move(int direction) {
        List<UIComponent> focusable = new ArrayList<>();
        collect(root, focusable);
        if (focusable.isEmpty()) return false;
        int current = focusable.indexOf(focused);
        int next = current < 0
                ? (direction > 0 ? 0 : focusable.size() - 1)
                : Math.floorMod(current + direction, focusable.size());
        focused = focusable.get(next);
        return true;
    }

    private void collect(UIComponent component, List<UIComponent> output) {
        if (component == null || !component.isVisible()) return;
        if (component.isFocusable()) output.add(component);
        for (UIComponent child : component.children()) collect(child, output);
    }

    private boolean belongsToRoot(UIComponent component) {
        UIComponent cursor = component;
        while (cursor != null && cursor.parent() != null) cursor = cursor.parent();
        return cursor == root;
    }
}

