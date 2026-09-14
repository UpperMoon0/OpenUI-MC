package com.nstut.openui.state;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ScopedAsyncTest {
    private static final Executor DIRECT = Runnable::run;

    @Test
    void keyedSubmitStartsOnlyOncePerMountedScope() {
        UiScope scope = new UiScope();
        AtomicInteger workCalls = new AtomicInteger();
        AtomicReference<Integer> delivered = new AtomicReference<>();

        ScopedAsync<Integer> first = ScopedAsync.submit(
                scope, "profile", workCalls::incrementAndGet, DIRECT, DIRECT, delivered::set, unexpectedFailure());
        ScopedAsync<Integer> second = ScopedAsync.submit(
                scope, "profile", workCalls::incrementAndGet, DIRECT, DIRECT, delivered::set, unexpectedFailure());

        assertSame(first, second);
        assertEquals(1, workCalls.get());
        assertEquals(1, delivered.get());
        scope.close();
        assertTrue(first.isClosed());
    }

    @Test
    void closingScopeSuppressesQueuedDelivery() {
        UiScope scope = new UiScope();
        AtomicReference<Runnable> delivery = new AtomicReference<>();
        AtomicInteger delivered = new AtomicInteger();
        Executor queuedDelivery = delivery::set;

        ScopedAsync.submit(scope, () -> 7, DIRECT, queuedDelivery,
                delivered::set, unexpectedFailure());
        assertNotNull(delivery.get());

        scope.close();
        delivery.get().run();
        assertEquals(0, delivered.get());
    }

    @Test
    void workFailureIsUnwrappedBeforeDelivery() {
        IllegalStateException expected = new IllegalStateException("boom");
        AtomicReference<Throwable> delivered = new AtomicReference<>();

        ScopedAsync<Integer> task = ScopedAsync.start(
                () -> { throw expected; },
                DIRECT,
                DIRECT,
                ignored -> fail("failure must not call success"),
                delivered::set);

        assertSame(expected, delivered.get());
        task.close();
    }

    @Test
    void closingBeforeQueuedBackgroundWorkPreventsWorkAndCallbacks() {
        AtomicReference<Runnable> background = new AtomicReference<>();
        AtomicInteger workCalls = new AtomicInteger();
        AtomicInteger callbacks = new AtomicInteger();
        Executor queuedBackground = background::set;

        ScopedAsync<Integer> task = ScopedAsync.start(
                workCalls::incrementAndGet,
                queuedBackground,
                DIRECT,
                ignored -> callbacks.incrementAndGet(),
                ignored -> callbacks.incrementAndGet());
        assertNotNull(background.get());

        task.close();
        background.get().run();

        assertEquals(0, workCalls.get());
        assertEquals(0, callbacks.get());
        assertTrue(task.isClosed());
    }

    @Test
    void closingRunningWorkRequestsInterruptionAndSuppressesCallbacks() throws Exception {
        ExecutorService background = Executors.newSingleThreadExecutor();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicInteger callbacks = new AtomicInteger();
        AtomicBoolean sawInterrupt = new AtomicBoolean();
        try {
            ScopedAsync<Integer> task = ScopedAsync.start(
                    () -> {
                        started.countDown();
                        try {
                            new CountDownLatch(1).await();
                            return 7;
                        } catch (InterruptedException expected) {
                            sawInterrupt.set(true);
                            interrupted.countDown();
                            throw new RuntimeException(expected);
                        }
                    },
                    background,
                    DIRECT,
                    ignored -> callbacks.incrementAndGet(),
                    ignored -> callbacks.incrementAndGet());

            assertTrue(started.await(2, TimeUnit.SECONDS));
            task.close();

            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
            assertTrue(sawInterrupt.get());
            assertEquals(0, callbacks.get());
            assertTrue(task.stage().toCompletableFuture().isCancelled());
        } finally {
            background.shutdownNow();
        }
    }

    private static Consumer<Throwable> unexpectedFailure() {
        return error -> fail("unexpected async failure: " + error);
    }
}
