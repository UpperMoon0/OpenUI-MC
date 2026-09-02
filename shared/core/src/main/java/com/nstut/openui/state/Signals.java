package com.nstut.openui.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Signals {
    private static final ThreadLocal<ArrayDeque<DependencyCollector>> TRACKERS =
            ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Batch> BATCH = ThreadLocal.withInitial(Batch::new);
    private static final ThreadLocal<ReadableSignal<?>> UPDATE_CAUSE = new ThreadLocal<>();

    private Signals() { }

    public static <T> Signal<T> of(T initialValue) {
        return new MutableSignal<>(null, initialValue);
    }

    /** Creates a signal with a stable debug name for inspector/dependency diagnostics. */
    public static <T> Signal<T> named(String debugName, T initialValue) {
        String name = Objects.requireNonNull(debugName, "debugName");
        if (name.isBlank()) throw new IllegalArgumentException("debugName cannot be blank");
        return new MutableSignal<>(name, initialValue);
    }

    public static <T> Computed<T> computed(Supplier<T> calculation) {
        return new ComputedSignal<>(calculation);
    }

    public static Effect effect(Runnable action) {
        EffectImpl effect = new EffectImpl(action);
        effect.run();
        return effect;
    }

    public static void batch(Runnable updates) {
        Batch batch = BATCH.get();
        batch.depth++;
        try {
            updates.run();
        } finally {
            if (--batch.depth == 0) batch.flush();
        }
    }

    /** The dependency that caused the currently rerunning effect, when available. */
    public static Optional<String> currentUpdateCause() {
        ReadableSignal<?> cause = UPDATE_CAUSE.get();
        return cause == null ? Optional.empty() : Optional.of(debug(cause).displayName());
    }

    /** Read-only signal diagnostics; querying them does not subscribe or track a dependency. */
    public static DebugSignal debug(ReadableSignal<?> signal) {
        Objects.requireNonNull(signal, "signal");
        if (signal instanceof MutableSignal<?> mutable) {
            return new DebugSignal(id(signal), mutable.debugName, "signal", mutable.listeners.size(), List.of());
        }
        if (signal instanceof ComputedSignal<?> computed) {
            return new DebugSignal(
                    id(signal),
                    null,
                    "computed",
                    computed.listeners.size(),
                    computed.dependencies.stream().map(Signals::debugLabel).toList());
        }
        return new DebugSignal(id(signal), null, signal.getClass().getSimpleName(), -1, List.of());
    }

    /** Dependencies currently tracked by an OpenUI effect. */
    public static List<DebugSignal> debugDependencies(Effect effect) {
        Objects.requireNonNull(effect, "effect");
        if (!(effect instanceof EffectImpl impl)) return List.of();
        return impl.dependencies.stream().map(Signals::debug).toList();
    }

    private static String debugLabel(ReadableSignal<?> signal) {
        return debug(signal).displayName();
    }

    private static String id(Object value) {
        return Integer.toHexString(System.identityHashCode(value));
    }

    private static void track(ReadableSignal<?> signal) {
        ArrayDeque<DependencyCollector> stack = TRACKERS.get();
        if (!stack.isEmpty()) stack.peek().accept(signal);
    }

    private static void notifyLater(Runnable notification) {
        Batch batch = BATCH.get();
        if (batch.depth > 0 || batch.flushing) batch.notifications.add(notification);
        else notification.run();
    }

    public record DebugSignal(String id, String name, String kind, int subscribers, List<String> dependencies) {
        public DebugSignal {
            Objects.requireNonNull(id, "id");
            kind = Objects.requireNonNullElse(kind, "signal");
            dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        }

        public String displayName() {
            return name != null && !name.isBlank() ? name : kind + "#" + id;
        }
    }

    private interface DependencyCollector {
        void accept(ReadableSignal<?> dependency);
    }

    private static final class Batch {
        private int depth;
        private boolean flushing;
        private final Set<Runnable> notifications = new LinkedHashSet<>();

        private void flush() {
            flushing = true;
            try {
                while (!notifications.isEmpty()) {
                    List<Runnable> pending = List.copyOf(notifications);
                    notifications.clear();
                    pending.forEach(Runnable::run);
                }
            } finally {
                flushing = false;
            }
        }
    }

    private static final class MutableSignal<T> implements Signal<T> {
        private final String debugName;
        private T value;
        private final List<Consumer<? super T>> listeners = new ArrayList<>();
        private final Runnable publisher = this::publish;

        private MutableSignal(String debugName, T value) {
            this.debugName = debugName;
            this.value = value;
        }

        @Override
        public T get() {
            track(this);
            return value;
        }

        @Override
        public void set(T value) {
            if (Objects.equals(this.value, value)) return;
            this.value = value;
            notifyLater(publisher);
        }

        private void publish() {
            for (Consumer<? super T> listener : List.copyOf(listeners)) listener.accept(value);
        }

        @Override
        public Subscription subscribe(Consumer<? super T> listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }
    }

    private static final class ComputedSignal<T> implements Computed<T>, DependencyCollector {
        private final Supplier<T> calculation;
        private final List<Consumer<? super T>> listeners = new ArrayList<>();
        private final List<Subscription> dependencySubscriptions = new ArrayList<>();
        private final Set<ReadableSignal<?>> dependencies = new LinkedHashSet<>();
        private boolean dirty = true;
        private boolean closed;
        private T value;

        private ComputedSignal(Supplier<T> calculation) {
            this.calculation = Objects.requireNonNull(calculation);
        }

        @Override
        public T get() {
            track(this);
            if (dirty) recompute();
            return value;
        }

        @Override
        public Subscription subscribe(Consumer<? super T> listener) {
            if (dirty) recompute();
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public void accept(ReadableSignal<?> dependency) {
            if (dependency != this) dependencies.add(dependency);
        }

        private void invalidate(Object ignored) {
            if (dirty || closed) return;
            dirty = true;
            notifyLater(() -> {
                T old = value;
                recompute();
                if (!Objects.equals(old, value)) {
                    for (Consumer<? super T> listener : List.copyOf(listeners)) listener.accept(value);
                }
            });
        }

        private void recompute() {
            if (closed) return;
            dependencySubscriptions.forEach(Subscription::close);
            dependencySubscriptions.clear();
            dependencies.clear();
            ArrayDeque<DependencyCollector> stack = TRACKERS.get();
            stack.push(this);
            try {
                value = calculation.get();
                dirty = false;
            } finally {
                stack.pop();
            }
            for (ReadableSignal<?> dependency : dependencies) {
                dependencySubscriptions.add(dependency.subscribe(this::invalidate));
            }
        }

        @Override
        public void close() {
            closed = true;
            dependencySubscriptions.forEach(Subscription::close);
            dependencySubscriptions.clear();
            listeners.clear();
        }
    }

    private static final class EffectImpl implements Effect, DependencyCollector {
        private final Runnable action;
        private final Set<ReadableSignal<?>> dependencies = new LinkedHashSet<>();
        private final List<Subscription> subscriptions = new ArrayList<>();
        private boolean closed;
        private boolean running;
        private ReadableSignal<?> nextCause;
        private final Runnable rerun = this::run;

        private EffectImpl(Runnable action) { this.action = Objects.requireNonNull(action); }

        @Override
        public void run() {
            if (closed || running) return;
            running = true;
            subscriptions.forEach(Subscription::close);
            subscriptions.clear();
            dependencies.clear();
            ArrayDeque<DependencyCollector> stack = TRACKERS.get();
            ReadableSignal<?> previousCause = UPDATE_CAUSE.get();
            ReadableSignal<?> cause = nextCause;
            nextCause = null;
            if (cause == null) UPDATE_CAUSE.remove();
            else UPDATE_CAUSE.set(cause);
            stack.push(this);
            try {
                action.run();
            } finally {
                stack.pop();
                if (previousCause == null) UPDATE_CAUSE.remove();
                else UPDATE_CAUSE.set(previousCause);
                running = false;
            }
            for (ReadableSignal<?> dependency : dependencies) {
                subscriptions.add(dependency.subscribe(ignored -> {
                    nextCause = dependency;
                    notifyLater(rerun);
                }));
            }
        }

        @Override
        public void accept(ReadableSignal<?> dependency) { dependencies.add(dependency); }

        @Override
        public void close() {
            closed = true;
            subscriptions.forEach(Subscription::close);
            subscriptions.clear();
        }
    }
}
