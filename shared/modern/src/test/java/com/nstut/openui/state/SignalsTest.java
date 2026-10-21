package com.nstut.openui.state;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalsTest {
    @Test
    void computedTracksDependenciesAndSkipsEqualValues() {
        Signal<Integer> count = Signals.of(2);
        Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);
        AtomicInteger notifications = new AtomicInteger();
        doubled.subscribe(value -> notifications.incrementAndGet());

        assertEquals(4, doubled.get());
        count.set(3);
        count.set(3);

        assertEquals(6, doubled.get());
        assertEquals(1, notifications.get());
    }

    @Test
    void batchRunsEffectsOnceWithTheFinalState() {
        Signal<Integer> left = Signals.of(1);
        Signal<Integer> right = Signals.of(2);
        AtomicInteger runs = new AtomicInteger();
        AtomicInteger observed = new AtomicInteger();
        Effect effect = Signals.effect(() -> {
            runs.incrementAndGet();
            observed.set(left.get() + right.get());
        });

        Signals.batch(() -> {
            left.set(10);
            right.set(20);
        });

        assertEquals(2, runs.get());
        assertEquals(30, observed.get());
        effect.close();
    }
}
