package com.nstut.openui.state;

import java.util.Objects;

public final class AsyncValue<T> {
    public enum Status { LOADING, SUCCESS, ERROR }
    private final Status status;
    private final T value;
    private final Throwable error;

    private AsyncValue(Status status, T value, Throwable error) {
        this.status = status;
        this.value = value;
        this.error = error;
    }

    public static <T> AsyncValue<T> loading() { return new AsyncValue<>(Status.LOADING, null, null); }
    public static <T> AsyncValue<T> success(T value) { return new AsyncValue<>(Status.SUCCESS, Objects.requireNonNull(value), null); }
    public static <T> AsyncValue<T> error(Throwable error) { return new AsyncValue<>(Status.ERROR, null, Objects.requireNonNull(error)); }
    public Status status() { return status; }
    public T value() { return value; }
    public Throwable error() { return error; }
}

