package com.nstut.openui.state;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SignalsDebugTest {
    private static final Executor DIRECT = Runnable::run;

    @Test
    void effectReportsNamedDependencyAndPreciseRerunCause() {
        Signal<Integer> value = Signals.named("state:counter", 0);
        AtomicReference<String> cause = new AtomicReference<>();

        try (Effect effect = Signals.effect(() -> {
            value.get();
            cause.set(Signals.currentUpdateCause().orElse("initial"));
        })) {
            assertEquals("initial", cause.get());
            assertEquals("state:counter", Signals.debugDependencies(effect).get(0).displayName());

            value.set(1);
            assertEquals("state:counter", cause.get());
        }
    }

    @Test
    void batchCoalescesOneEffectRerunAcrossMultipleDependencies() {
        Signal<Integer> first = Signals.named("state:first", 0);
        Signal<Integer> second = Signals.named("state:second", 0);
        AtomicInteger runs = new AtomicInteger();

        Effect effect = Signals.effect(() -> {
            first.get();
            second.get();
            runs.incrementAndGet();
        });
        try {
            assertEquals(1, runs.get());
            assertEquals(1, Signals.debug(first).subscribers());
            assertEquals(1, Signals.debug(second).subscribers());
            assertEquals(List.of("state:first", "state:second"),
                    Signals.debugDependencies(effect).stream().map(Signals.DebugSignal::displayName).toList());

            Signals.batch(() -> {
                first.set(1);
                second.set(1);
            });

            assertEquals(2, runs.get(), "one batch should trigger one effect rerun");
        } finally {
            effect.close();
        }

        assertEquals(0, Signals.debug(first).subscribers());
        assertEquals(0, Signals.debug(second).subscribers());
    }

    @Test
    void computedDebugSnapshotExposesDependencyGraphAndCloseUnsubscribes() {
        Signal<Integer> source = Signals.named("state:source", 2);
        Computed<Integer> doubled = Signals.computed(() -> source.get() * 2);

        assertEquals(4, doubled.get());
        Signals.DebugSignal debug = Signals.debug(doubled);
        assertEquals("computed", debug.kind());
        assertEquals(List.of("state:source"), debug.dependencies());
        assertEquals(1, Signals.debug(source).subscribers());

        doubled.close();
        assertEquals(0, Signals.debug(source).subscribers());
    }

    @Test
    void scopeSnapshotIncludesStateEffectsSubscriptionsAsyncAndKeyedResources() {
        UiScope scope = new UiScope();
        Signal<Integer> state = scope.remember("query", 1);
        scope.effect(state::get);
        scope.subscribe(state, ignored -> { });
        scope.own("resource", () -> () -> { });
        ScopedAsync.submit(scope, "load", () -> 7, DIRECT, DIRECT,
                ignored -> { }, error -> fail("unexpected async failure", error));

        UiScope.DebugSnapshot debug = scope.debugSnapshot();
        assertFalse(debug.closed());
        assertEquals(1, debug.effects());
        assertEquals(1, debug.subscriptions());
        assertEquals(1, debug.asyncTasks());
        assertEquals(4, debug.resources());
        assertEquals(1, debug.rememberedState());
        assertEquals(2, debug.keyedResources());
        assertTrue(debug.signals().stream().anyMatch(signal -> signal.displayName().equals("state:query")));

        scope.close();

        UiScope.DebugSnapshot closed = scope.debugSnapshot();
        assertTrue(closed.closed());
        assertEquals(0, closed.resources());
        assertEquals(0, closed.rememberedState());
        assertEquals(0, closed.keyedResources());
        assertTrue(closed.signals().isEmpty());
    }

    @Test
    void namedSignalsRejectBlankDebugNames() {
        assertThrows(IllegalArgumentException.class, () -> Signals.named("   ", 1));
    }
}
