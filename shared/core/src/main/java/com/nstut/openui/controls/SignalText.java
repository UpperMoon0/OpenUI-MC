package com.nstut.openui.controls;

import com.nstut.openui.api.TextWidget;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Subscription;

public final class SignalText extends TextWidget {
    private final ReadableSignal<String> signal;
    private Subscription subscription = Subscription.EMPTY;

    /** Theme-native signal text. The inherited TextWidget resolves its color from the active theme. */
    public SignalText(ReadableSignal<String> signal) {
        super(signal.get());
        this.signal = signal;
    }

    /** Explicit-color compatibility constructor. */
    public SignalText(ReadableSignal<String> signal, int color, boolean centered) {
        super(signal.get(), color, centered);
        this.signal = signal;
    }

    @Override protected void onMount() {
        subscription = signal.subscribe(this::setText);
        setText(signal.get());
    }

    @Override protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
    }
}
