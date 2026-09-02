package com.nstut.openui.state;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Async work whose execution and delivery are owned by a UI scope. */
public final class ScopedAsync<T> implements AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean();
    private final CompletableFuture<T> completion = new CompletableFuture<>();
    private volatile FutureTask<T> runner;

    private ScopedAsync() { }

    /**
     * Starts interrupt-capable async work without attaching it to a scope yet.
     * Closing requests interruption of already-running work and always suppresses
     * callbacks that have not been delivered yet.
     */
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

        ScopedAsync<T> task = new ScopedAsync<>();
        FutureTask<T> runner = new FutureTask<>(work::get) {
            @Override
            protected void done() {
                task.completeFrom(this, delivery, success, failure);
            }
        };
        task.runner = runner;
        try {
            background.execute(runner);
        } catch (RuntimeException rejected) {
            task.runner = null;
            task.completeFailure(rejected, delivery, failure);
        }
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

    public CompletionStage<T> stage() { return completion; }
    public boolean isClosed() { return closed.get(); }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        FutureTask<T> current = runner;
        if (current != null) current.cancel(true);
        completion.cancel(false);
    }

    private void completeFrom(
            FutureTask<T> source,
            Executor delivery,
            Consumer<? super T> success,
            Consumer<? super Throwable> failure) {
        if (closed.get() || source.isCancelled()) {
            completion.cancel(false);
            return;
        }
        try {
            T value = source.get();
            completion.complete(value);
            deliver(delivery, () -> success.accept(value));
        } catch (CancellationException cancelled) {
            completion.cancel(false);
        } catch (ExecutionException failed) {
            completeFailure(unwrap(failed), delivery, failure);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            completeFailure(interrupted, delivery, failure);
        }
    }

    private void completeFailure(Throwable error, Executor delivery, Consumer<? super Throwable> failure) {
        completion.completeExceptionally(error);
        deliver(delivery, () -> failure.accept(error));
    }

    private void deliver(Executor delivery, Runnable callback) {
        if (closed.get()) return;
        try {
            delivery.execute(() -> {
                if (!closed.get()) callback.run();
            });
        } catch (RuntimeException ignored) {
            // The work stage already reflects its result. A rejected delivery
            // executor cannot safely run UI callbacks on this background thread.
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable cause = error.getCause();
        return cause != null ? cause : error;
    }
}
