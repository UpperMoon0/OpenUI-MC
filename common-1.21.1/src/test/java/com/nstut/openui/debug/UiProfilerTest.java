package com.nstut.openui.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiProfilerTest {
    @Test
    void disabledProfilerHasZeroCostSurfaceAndRetainsNothing() {
        UiProfiler profiler = new UiProfiler();

        assertFalse(profiler.enabled());
        assertEquals(0L, profiler.begin());
        profiler.record(this, UiProfiler.Phase.BUILD, 0L, "ignored");

        assertTrue(profiler.snapshot().isEmpty());
        assertTrue(profiler.trace().isEmpty());
        assertTrue(profiler.warnings().isEmpty());
    }

    @Test
    void traceIsBoundedAndUpdateCountTracksOnlyBuilds() {
        UiProfiler profiler = new UiProfiler();
        profiler.enabled(true);
        profiler.traceLimit(2);
        Object owner = new Object();

        profiler.record(owner, UiProfiler.Phase.BUILD, System.nanoTime() - 1_000L, "signal changed");
        profiler.record(owner, UiProfiler.Phase.LAYOUT, System.nanoTime() - 1_000L, "layout cause ignored");
        profiler.record(owner, UiProfiler.Phase.PAINT, System.nanoTime() - 1_000L, null);

        UiProfiler.Snapshot snapshot = profiler.snapshot().get(owner);
        assertEquals(2, profiler.trace().size());
        assertEquals(1, snapshot.updates());
        assertEquals("signal changed", snapshot.lastCause());
        assertTrue(snapshot.buildNanos() > 0);
        assertTrue(snapshot.layoutNanos() > 0);
        assertTrue(snapshot.paintNanos() > 0);
    }

    @Test
    void everyProfilerPhaseAccumulatesWithoutInflatingBuildCount() {
        UiProfiler profiler = new UiProfiler();
        profiler.enabled(true);
        Object owner = new Object();

        for (UiProfiler.Phase phase : UiProfiler.Phase.values()) {
            profiler.record(owner, phase, System.nanoTime() - 1_000L,
                    phase == UiProfiler.Phase.BUILD ? "state:query" : "ignored");
        }

        UiProfiler.Snapshot snapshot = profiler.snapshot().get(owner);
        assertEquals(1, snapshot.updates());
        assertEquals("state:query", snapshot.lastCause());
        assertTrue(snapshot.buildNanos() > 0);
        assertTrue(snapshot.reconcileNanos() > 0);
        assertTrue(snapshot.layoutNanos() > 0);
        assertTrue(snapshot.paintNanos() > 0);
        assertTrue(snapshot.eventNanos() > 0);
    }

    @Test
    void slowSamplesProduceInspectorWarningsWithoutSleeping() {
        UiProfiler profiler = new UiProfiler();
        profiler.enabled(true);
        profiler.record(this, UiProfiler.Phase.LAYOUT, System.nanoTime() - 10_000_000L, "test");

        assertFalse(profiler.warnings().isEmpty());
        assertEquals(UiProfiler.Phase.LAYOUT, profiler.warnings().get(0).phase());
    }

    @Test
    void disablingProfilerClearsDiagnosticsAndTraceLimitIsValidated() {
        UiProfiler profiler = new UiProfiler();
        profiler.enabled(true);
        profiler.record(this, UiProfiler.Phase.EVENT, System.nanoTime() - 1_000L, "click");
        assertFalse(profiler.trace().isEmpty());

        profiler.enabled(false);

        assertTrue(profiler.snapshot().isEmpty());
        assertTrue(profiler.trace().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> profiler.traceLimit(0));
    }
}
