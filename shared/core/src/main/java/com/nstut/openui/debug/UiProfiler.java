package com.nstut.openui.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Opt-in low-overhead profiler backing Devtools 2.0. Callers use stable object
 * identities (normally components) and can attach an update cause plus timing.
 */
public final class UiProfiler {
    private static final int DEFAULT_TRACE_LIMIT = 512;
    private static final long SLOW_BUILD_NANOS = 2_000_000L;
    private static final long SLOW_LAYOUT_NANOS = 4_000_000L;
    private static final long SLOW_PAINT_NANOS = 4_000_000L;
    private static final long SLOW_EVENT_NANOS = 1_000_000L;

    // Stats must not keep reconciled/unmounted component owners alive forever.
    // The bounded trace may retain its current window, while older owners can GC.
    private final Map<Object, MutableStats> stats = Collections.synchronizedMap(new WeakHashMap<>());
    private final Deque<TraceEntry> trace = new ArrayDeque<>();
    private volatile boolean enabled;
    private volatile int traceLimit = DEFAULT_TRACE_LIMIT;

    public boolean enabled() { return enabled; }
    public void enabled(boolean value) {
        enabled = value;
        if (!value) clear();
    }

    public int traceLimit() { return traceLimit; }
    public void traceLimit(int value) {
        if (value < 1) throw new IllegalArgumentException("traceLimit must be >= 1");
        traceLimit = value;
        trimTrace();
    }

    public long begin() { return enabled ? System.nanoTime() : 0L; }

    public void record(Object owner, Phase phase, long startedNanos, String cause) {
        if (!enabled) return;
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(phase, "phase");
        long elapsed = Math.max(0L, System.nanoTime() - startedNanos);
        MutableStats ownerStats;
        synchronized (stats) {
            ownerStats = stats.computeIfAbsent(owner, ignored -> new MutableStats());
        }
        ownerStats.record(phase, elapsed, cause);
        synchronized (trace) {
            trace.addLast(new TraceEntry(System.nanoTime(), owner, phase, elapsed, cause));
            while (trace.size() > traceLimit) trace.removeFirst();
        }
    }

    public Map<Object, Snapshot> snapshot() {
        Map<Object, Snapshot> copy = new LinkedHashMap<>();
        synchronized (stats) {
            stats.forEach((key, value) -> copy.put(key, value.snapshot()));
        }
        return Map.copyOf(copy);
    }

    /** Ordered newest-window event/build/layout/paint trace for inspector tooling. */
    public List<TraceEntry> trace() {
        synchronized (trace) {
            return List.copyOf(trace);
        }
    }

    /** Default-safe heuristics for common expensive UI work. */
    public List<Warning> warnings() {
        List<Warning> warnings = new ArrayList<>();
        for (TraceEntry entry : trace()) {
            long threshold = switch (entry.phase()) {
                case BUILD, RECONCILE -> SLOW_BUILD_NANOS;
                case LAYOUT -> SLOW_LAYOUT_NANOS;
                case PAINT -> SLOW_PAINT_NANOS;
                case EVENT -> SLOW_EVENT_NANOS;
            };
            if (entry.durationNanos() >= threshold) {
                warnings.add(new Warning(entry.owner(), entry.phase(), entry.durationNanos(),
                        "Slow " + entry.phase().name().toLowerCase() + " sample"));
            }
        }
        return List.copyOf(warnings);
    }

    public void clear() {
        stats.clear();
        synchronized (trace) { trace.clear(); }
    }

    private void trimTrace() {
        synchronized (trace) {
            while (trace.size() > traceLimit) trace.removeFirst();
        }
    }

    public enum Phase { BUILD, RECONCILE, LAYOUT, PAINT, EVENT }

    /** lastCause is specifically the most recent declarative BUILD trigger. */
    public record Snapshot(long buildNanos, long reconcileNanos, long layoutNanos,
                           long paintNanos, long eventNanos, long updates, String lastCause) { }

    public record TraceEntry(long timestampNanos, Object owner, Phase phase, long durationNanos, String cause) { }
    public record Warning(Object owner, Phase phase, long durationNanos, String message) { }

    private static final class MutableStats {
        private long build, reconcile, layout, paint, event, updates;
        private String lastCause;
        synchronized void record(Phase phase, long nanos, String cause) {
            switch (phase) {
                case BUILD -> {
                    build += nanos;
                    updates++;
                    if (cause != null && !cause.isBlank()) lastCause = cause;
                }
                case RECONCILE -> reconcile += nanos;
                case LAYOUT -> layout += nanos;
                case PAINT -> paint += nanos;
                case EVENT -> event += nanos;
            }
        }
        synchronized Snapshot snapshot() {
            return new Snapshot(build, reconcile, layout, paint, event, updates, lastCause);
        }
    }
}
