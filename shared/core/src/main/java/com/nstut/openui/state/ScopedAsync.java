package com.nstut.openui.state;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Async work whose delivery is suppressed after its owning UI scope closes. */
public final class ScopedAsync<T> implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CompletableFuture<T> future;

    private ScopedAsync(CompletableFuture<T> future) {
        this.future = future;
    }

    /** Starts cancellable async work without attaching it to a scope yet. */
    public static <T> ScopedAsync<T> start(
            Supplier<? extends T> work,
            Executor background,
            Executor delivery,
            Consumer<? super T> success,
            Consumer<? super Throwable> failure) {
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(background, "background");
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(success, "success");
        Objects.requireNonNull(failure, "failure");

        CompletableFuture<T> future = CompletableFuture.supplyAsync(work::get, background);
        ScopedAsync<T> task = new ScopedAsync<>(future);
        future.whenCompleteAsync((value, error) -> {
            if (task.closed.get()) return;
            if (error == null) success.accept(value);
            else failure.accept(unwrap(error));
        }, delivery);
        return task;
    }

    /** Starts work and owns it for the lifetime of the supplied scope. */
    public static <T> ScopedAsync<T> submit(
            UiScope scope,
            Supplier<? extends T> work,
            Executor background,
            Executor delivery,
            Consumer<? super T> success,
            Consumer<? super Throwable> failure) {
        Objects.requireNonNull(scope, "scope");
        return scope.own(start(work, background, delivery, success, failure));
    }

    /**
     * Starts at most one task for {@code key} in this mount. Repeated calls from
     * a declarative rebuild reuse the existing task instead of duplicating work.
     */
    public static <T> ScopedAsync<T> submit(
            UiScope scope,
            String key,
            Supplier<? extends T> work,
            Executor background,
            Executor delivery,
            Consumer<? super T> success,
            Consumer<? super Throwable> failure) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(key, "key");
        return scope.own("async:" + key, () -> start(work, background, delivery, success, failure));
    }

    public CompletionStage<T> stage() { return future; }
    public boolean isClosed() { return closed.get(); }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        future.cancel(true);
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error.getCause();
        return cause != null ? cause : error;
    }
}
