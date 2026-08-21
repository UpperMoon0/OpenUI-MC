package com.nstut.openui.state;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class SelectionModel<T> {
    public enum Mode { NONE, SINGLE, MULTIPLE }
    private final Mode mode;
    private final Signal<Set<T>> selection = Signals.of(Set.of());

    public SelectionModel(Mode mode) { this.mode = mode; }
    public static <T> SelectionModel<T> single() { return new SelectionModel<>(Mode.SINGLE); }
    public static <T> SelectionModel<T> multiple() { return new SelectionModel<>(Mode.MULTIPLE); }
    public ReadableSignal<Set<T>> selection() { return selection; }
    public boolean isSelected(T value) { return selection.get().contains(value); }

    public void select(T value) {
        if (mode == Mode.NONE) return;
        if (mode == Mode.SINGLE) selection.set(Set.of(value));
        else {
            Set<T> next = new LinkedHashSet<>(selection.get());
            next.add(value);
            selection.set(Collections.unmodifiableSet(next));
        }
    }

    public void toggle(T value) { if (isSelected(value)) deselect(value); else select(value); }
    public void deselect(T value) {
        Set<T> next = new LinkedHashSet<>(selection.get());
        next.remove(value);
        selection.set(Collections.unmodifiableSet(next));
    }
    public void clear() { selection.set(Set.of()); }
}

