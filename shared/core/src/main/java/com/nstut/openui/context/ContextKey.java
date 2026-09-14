package com.nstut.openui.context;

import java.util.Objects;

/**
 * Identity-based typed key for inherited OpenUI context values.
 *
 * <p>Keys intentionally do not override equals/hashCode: two keys with the
 * same debug name are still different capabilities.</p>
 */
public final class ContextKey<T> {
    private final String debugName;

    private ContextKey(String debugName) {
        this.debugName = Objects.requireNonNull(debugName, "debugName");
    }

    public static <T> ContextKey<T> create(String debugName) {
        return new ContextKey<>(debugName);
    }

    public String debugName() {
        return debugName;
    }

    @Override
    public String toString() {
        return "ContextKey[" + debugName + "]";
    }
}
