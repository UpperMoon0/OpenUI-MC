package com.nstut.openui.state;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
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

    private static Consumer<Throwable> unexpectedFailure() {
        return error -> fail("unexpected async failure: " + error);
    }
}
