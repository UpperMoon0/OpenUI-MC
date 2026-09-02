package com.nstut.openui.debug;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opt-in low-overhead profiler backing Devtools 2.0. Callers use stable object
 * identities (normally components) and can attach an update cause plus timing.
 */
public final class UiProfiler {
    private final Map<Object, MutableStats> stats = new ConcurrentHashMap<>();
    private volatile boolean enabled;

    public boolean enabled() { return enabled; }
    public void enabled(boolean value) { enabled = value; if (!value) stats.clear(); }

    public long begin() { return enabled ? System.nanoTime() : 0L; }

    public void record(Object owner, Phase phase, long startedNanos, String cause) {
        if (!enabled) return;
        Objects.requireNonNull(owner, "owner");
        long elapsed = Math.max(0L, System.nanoTime() - startedNanos);
        stats.computeIfAbsent(owner, ignored -> new MutableStats()).record(phase, elapsed, cause);
    }

    public Map<Object, Snapshot> snapshot() {
        Map<Object, Snapshot> copy = new LinkedHashMap<>();
        stats.forEach((key, value) -> copy.put(key, value.snapshot()));
        return Map.copyOf(copy);
    }

    public void clear() { stats.clear(); }

    public enum Phase { BUILD, RECONCILE, LAYOUT, PAINT, EVENT }

    public record Snapshot(long buildNanos, long reconcileNanos, long layoutNanos,
                           long paintNanos, long eventNanos, long updates, String lastCause) { }

    private static final class MutableStats {
        private long build, reconcile, layout, paint, event, updates;
        private String lastCause;
        synchronized void record(Phase phase, long nanos, String cause) {
            switch (phase) {
                case BUILD -> build += nanos;
                case RECONCILE -> reconcile += nanos;
                case LAYOUT -> layout += nanos;
                case PAINT -> paint += nanos;
                case EVENT -> event += nanos;
            }
            updates++;
            if (cause != null && !cause.isBlank()) lastCause = cause;
        }
        synchronized Snapshot snapshot() {
            return new Snapshot(build, reconcile, layout, paint, event, updates, lastCause);
        }
    }
}
