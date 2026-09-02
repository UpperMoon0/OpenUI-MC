package com.nstut.openui.runtime;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Small deterministic scheduler for coalescing framework work until the next
 * runtime flush. Duplicate keyed work is executed once in insertion order.
 */
public final class FrameScheduler {
    private final Queue<Runnable> immediate = new ArrayDeque<>();
    private final Set<KeyedTask> keyed = new LinkedHashSet<>();
    private boolean flushing;
    private int maxPasses = 100;

    public void schedule(Runnable task) {
        immediate.add(Objects.requireNonNull(task, "task"));
    }

    public void schedule(Object key, Runnable task) {
        keyed.add(new KeyedTask(Objects.requireNonNull(key, "key"), Objects.requireNonNull(task, "task")));
    }

    public boolean hasPendingWork() {
        return !immediate.isEmpty() || !keyed.isEmpty();
    }

    public void maxPasses(int value) {
        if (value < 1) throw new IllegalArgumentException("maxPasses must be >= 1");
        maxPasses = value;
    }

    public void flush() {
        if (flushing) return;
        flushing = true;
        try {
            int passes = 0;
            while (hasPendingWork()) {
                if (++passes > maxPasses) {
                    immediate.clear();
                    keyed.clear();
                    throw new IllegalStateException("OpenUI update loop exceeded " + maxPasses + " scheduler passes");
                }

                while (!immediate.isEmpty()) immediate.remove().run();
                if (!keyed.isEmpty()) {
                    var batch = java.util.List.copyOf(keyed);
                    keyed.clear();
                    for (KeyedTask task : batch) task.task.run();
                }
            }
        } finally {
            flushing = false;
        }
    }

    private static final class KeyedTask {
        private final Object key;
        private final Runnable task;

        private KeyedTask(Object key, Runnable task) {
            this.key = key;
            this.task = task;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof KeyedTask task && key.equals(task.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }
}
