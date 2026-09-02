package com.nstut.openui.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UiProfilerTest {
    @Test
    void traceIsBoundedAndUpdateCountTracksBuilds() {
        UiProfiler profiler = new UiProfiler();
        profiler.enabled(true);
        profiler.traceLimit(2);
        Object owner = new Object();

        profiler.record(owner, UiProfiler.Phase.BUILD, System.nanoTime(), "signal changed");
        profiler.record(owner, UiProfiler.Phase.LAYOUT, System.nanoTime(), null);
        profiler.record(owner, UiProfiler.Phase.PAINT, System.nanoTime(), null);

        assertEquals(2, profiler.trace().size());
        assertEquals(1, profiler.snapshot().get(owner).updates());
        assertEquals("signal changed", profiler.snapshot().get(owner).lastCause());
    }

    @Test
    void slowSamplesProduceInspectorWarnings() {
        UiProfiler profiler = new UiProfiler();
        profiler.enabled(true);
        profiler.record(this, UiProfiler.Phase.LAYOUT, System.nanoTime() - 10_000_000L, "test");

        assertFalse(profiler.warnings().isEmpty());
        assertEquals(UiProfiler.Phase.LAYOUT, profiler.warnings().get(0).phase());
    }
}
