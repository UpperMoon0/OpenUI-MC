package com.nstut.openui.controls;

import com.nstut.openui.api.Stack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SignalSwitcher<T> extends Stack {
    private final ReadableSignal<T> selection;
    private final Map<T, Supplier<UIComponent>> routes = new LinkedHashMap<>();
    private Subscription subscription = Subscription.EMPTY;
    private T displayed;

    public SignalSwitcher(ReadableSignal<T> selection) { this.selection = selection; }
    public SignalSwitcher<T> when(T value, Supplier<UIComponent> builder) { routes.put(value, builder); refresh(); return this; }
    @Override protected void onMount() { subscription = selection.subscribe(ignored -> refresh()); refresh(); }
    @Override protected void onUnmount() { subscription.close(); subscription = Subscription.EMPTY; }
    private void refresh() {
        T current = selection.get();
        Supplier<UIComponent> builder = routes.get(current);
        // Routes are normally registered through a fluent when(...).when(...) chain.
        // Do not remember a selection before its route has been registered: doing so
        // leaves a rebuilt screen blank when the active route is not the first route.
        if (builder == null) return;
        if (java.util.Objects.equals(current, displayed) && !children.isEmpty()) return;
        displayed = current;
        clearChildren();
        addChild(builder.get());
    }
}

