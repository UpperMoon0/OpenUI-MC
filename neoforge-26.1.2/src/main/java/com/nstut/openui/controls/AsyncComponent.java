package com.nstut.openui.controls;

import com.nstut.openui.api.Stack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.state.AsyncValue;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;

import java.util.function.Function;
import java.util.function.Supplier;

public final class AsyncComponent<T> extends Stack {
    private final ReadableSignal<AsyncValue<T>> state;
    private final Supplier<UIComponent> loading;
    private final Function<T, UIComponent> success;
    private final Function<Throwable, UIComponent> error;
    private Subscription subscription = Subscription.EMPTY;

    public AsyncComponent(ReadableSignal<AsyncValue<T>> state, Supplier<UIComponent> loading,
                          Function<T, UIComponent> success, Function<Throwable, UIComponent> error) {
        this.state = state; this.loading = loading; this.success = success; this.error = error; refresh(state.get());
    }
    @Override protected void onMount() { subscription = state.subscribe(this::refresh); }
    @Override protected void onUnmount() { subscription.close(); subscription = Subscription.EMPTY; }
    private void refresh(AsyncValue<T> value) {
        clearChildren();
        addChild(switch (value.status()) {
            case LOADING -> loading.get();
            case SUCCESS -> success.apply(value.value());
            case ERROR -> error.apply(value.error());
        });
    }
}

