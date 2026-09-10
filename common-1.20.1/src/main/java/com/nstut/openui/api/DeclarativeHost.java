package com.nstut.openui.api;

import com.nstut.openui.debug.UiProfiler;
import com.nstut.openui.declarative.DeclarativeChild;
import com.nstut.openui.declarative.DeclarativeTree;
import com.nstut.openui.input.EventPhase;
import com.nstut.openui.input.UiEvent;
import com.nstut.openui.runtime.FrameScheduler;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signals;
import com.nstut.openui.state.UiScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Reactive functional-component bridge backed by retained UIComponent nodes. */
public final class DeclarativeHost extends ScopedUIComponent {
    @FunctionalInterface
    public interface Builder {
        List<DeclarativeChild<UIComponent>> build(UiBuildScope scope);
    }

    private static final Map<UiRuntime, FrameScheduler> RUNTIME_SCHEDULERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final DeclarativeTree.Adapter<UIComponent> RETAINED_ADAPTER = new DeclarativeTree.Adapter<>() {
        @Override public void attach(UIComponent parent, UIComponent child) { parent.addChild(child); }
        @Override public void detach(UIComponent parent, UIComponent child) { parent.removeChild(child); }
        @Override public List<UIComponent> children(UIComponent parent) { return parent.children(); }
        @Override public void reorder(UIComponent parent, List<UIComponent> orderedChildren) {
            parent.children.clear();
            parent.children.addAll(orderedChildren);
        }
    };

    private final Builder builder;
    private final DeclarativeTree<UIComponent> tree = new DeclarativeTree<>(RETAINED_ADAPTER);
    private final UiProfiler profiler = new UiProfiler();
    private MountState activeMount;

    private static final class MountState {
        private final UiRuntime runtime;
        private final Object buildScheduleKey = new Object();
        private final Object reconcileScheduleKey = new Object();
        private List<DeclarativeChild<UIComponent>> pending = List.of();

        private MountState(UiRuntime runtime) {
            this.runtime = Objects.requireNonNull(runtime, "runtime");
        }
    }

    public DeclarativeHost(Builder builder) {
        this.builder = Objects.requireNonNull(builder, "builder");
    }

    /** Backward-compatible no-argument builder form. */
    public DeclarativeHost(Supplier<? extends List<DeclarativeChild<UIComponent>>> builder) {
        this(scope -> Objects.requireNonNull(builder, "builder").get());
    }

    public UiProfiler profiler() { return profiler; }
    public DeclarativeTree.Diagnostics reconcileDiagnostics() { return tree.diagnostics(); }

    @Override
    protected void onScopedMount(UiScope scope) {
        MountState mount = new MountState(runtime());
        activeMount = mount;
        UiBuildScope currentBuildScope = new UiBuildScope(scope, this);
        scope.effect(() -> {
            if (!isActiveMount(mount)) return;
            long started = profiler.begin();
            String cause = Signals.currentUpdateCause().orElse("initial mount");
            List<DeclarativeChild<UIComponent>> next = builder.build(currentBuildScope);
            mount.pending = next == null ? List.of() : List.copyOf(next);
            profiler.record(this, UiProfiler.Phase.BUILD, started, cause);
            schedulePending(mount);
        }, task -> scheduleBuild(mount, task));
    }

    @Override
    protected void onScopedUnmount() {
        activeMount = null;
    }

    private boolean isActiveMount(MountState mount) {
        return activeMount == mount && runtime() == mount.runtime;
    }

    private void scheduleBuild(MountState mount, Runnable task) {
        if (!isActiveMount(mount)) return;
        invalidateBuild();
        schedulerFor(mount.runtime).schedule(mount.buildScheduleKey, () -> {
            if (isActiveMount(mount)) task.run();
        });
    }

    private void schedulePending(MountState mount) {
        if (!isActiveMount(mount)) return;
        schedulerFor(mount.runtime).schedule(mount.reconcileScheduleKey, () -> {
            if (isActiveMount(mount)) applyPending(mount);
        });
    }

    private void ensureBuilt() {
        UiRuntime currentRuntime = runtime();
        if (currentRuntime != null) schedulerFor(currentRuntime).flush();
    }

    private static FrameScheduler schedulerFor(UiRuntime runtime) {
        synchronized (RUNTIME_SCHEDULERS) {
            return RUNTIME_SCHEDULERS.computeIfAbsent(runtime, ignored -> new FrameScheduler());
        }
    }

    private void applyPending(MountState mount) {
        if (!isActiveMount(mount)) return;
        long started = profiler.begin();
        tree.reconcile(this, mount.pending);
        markBuilt();
        DeclarativeTree.Diagnostics d = tree.diagnostics();
        profiler.record(this, UiProfiler.Phase.RECONCILE, started,
                "created=" + d.created() + " reused=" + d.reused() + " removed=" + d.removed());
        invalidateLayout();
    }

    @Override
    public void dispatchEvent(UiEvent event, EventPhase phase) {
        long started = profiler.begin();
        try {
            super.dispatchEvent(event, phase);
        } finally {
            Object owner = event.target() != null ? event.target() : this;
            profiler.record(owner, UiProfiler.Phase.EVENT, started, event.type() + "/" + phase);
        }
    }

    @Override public int preferredWidth(Font font) {
        ensureBuilt();
        int result = 0;
        for (UIComponent child : children) result = Math.max(result, child.preferredWidth(font));
        return result;
    }

    @Override public int preferredHeight(Font font) {
        ensureBuilt();
        int result = 0;
        for (UIComponent child : children) result = Math.max(result, child.preferredHeight(font));
        return result;
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        long started = profiler.begin();
        ensureBuilt();
        setBounds(x, y, availableWidth, availableHeight);
        for (UIComponent child : children) child.layout(x, y, availableWidth, availableHeight);
        profiler.record(this, UiProfiler.Phase.LAYOUT, started, null);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        long started = profiler.begin();
        renderChildren(g, font, mx, my, pt);
        profiler.record(this, UiProfiler.Phase.PAINT, started, null);
    }
}
