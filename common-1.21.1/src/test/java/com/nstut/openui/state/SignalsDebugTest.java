package com.nstut.openui.state;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SignalsDebugTest {
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
    void computedDebugSnapshotExposesDependencyGraph() {
        Signal<Integer> source = Signals.named("state:source", 2);
        Computed<Integer> doubled = Signals.computed(() -> source.get() * 2);

        assertEquals(4, doubled.get());
        Signals.DebugSignal debug = Signals.debug(doubled);
        assertEquals("computed", debug.kind());
        assertEquals(java.util.List.of("state:source"), debug.dependencies());
        doubled.close();
    }

    @Test
    void scopeSnapshotIncludesResourcesStateAndEffectDependencies() {
        UiScope scope = new UiScope();
        Signal<Integer> state = scope.remember("query", 1);
        scope.effect(state::get);
        scope.subscribe(state, ignored -> { });

        UiScope.DebugSnapshot debug = scope.debugSnapshot();
        assertEquals(1, debug.effects());
        assertEquals(1, debug.subscriptions());
        assertEquals(2, debug.resources());
        assertEquals(1, debug.rememberedState());
        assertTrue(debug.signals().stream().anyMatch(signal -> signal.displayName().equals("state:query")));

        scope.close();
    }
}
