package com.nstut.openui.state;

import java.util.function.Consumer;

public interface ReadableSignal<T> {
    T get();
    Subscription subscribe(Consumer<? super T> listener);
}

