package com.nstut.openui.navigation;

import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;

import java.util.ArrayList;
import java.util.List;

public final class Navigator {
    private final List<Route<?>> stack = new ArrayList<>();
    private final Signal<Route<?>> current;
    public Navigator(Route<?> initial) { stack.add(initial); current = Signals.of(initial); }
    public ReadableSignal<Route<?>> current() { return current; }
    public void push(Route<?> route) { stack.add(route); current.set(route); }
    public boolean canPop() { return stack.size() > 1; }
    public boolean pop() { if (!canPop()) return false; stack.remove(stack.size() - 1); current.set(stack.get(stack.size() - 1)); return true; }
    public void replace(Route<?> route) { stack.set(stack.size() - 1, route); current.set(route); }
    public int depth() { return stack.size(); }
}

