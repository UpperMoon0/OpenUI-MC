package com.nstut.openui.api;

import com.nstut.openui.debug.UiProfiler;
import com.nstut.openui.declarative.DeclarativeChild;
import com.nstut.openui.declarative.DeclarativeTree;
import com.nstut.openui.input.EventPhase;
import com.nstut.openui.input.UiEvent;
import com.nstut.openui.runtime.FrameScheduler;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.UiScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Reactive declarative bridge backed by the retained 26.1.2 component tree. */
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
    private final FrameScheduler detachedScheduler = new FrameScheduler();
    private final DeclarativeTree<UIComponent> tree = new DeclarativeTree<>(RETAINED_ADAPTER);
    private final UiProfiler profiler = new UiProfiler();
    private List<DeclarativeChild<UIComponent>> pending = List.of();

    public DeclarativeHost(Builder builder) {
        this.builder = Objects.requireNonNull(builder, "builder");
    }

    public DeclarativeHost(Supplier<? extends List<DeclarativeChild<UIComponent>>> builder) {
        this(scope -> Objects.requireNonNull(builder, "builder").get());
    }

    public UiProfiler profiler() { return profiler; }

    @Override
    protected void onScopedMount(UiScope scope) {
        UiBuildScope currentBuildScope = new UiBuildScope(scope, this);
        scope.effect(() -> {
            long started = profiler.begin();
            List<DeclarativeChild<UIComponent>> next = builder.build(currentBuildScope);
            pending = next == null ? List.of() : List.copyOf(next);
            profiler.record(this, UiProfiler.Phase.BUILD, started, "signal/context dependency changed");
            schedulePending();
            invalidateBuild();
        });
    }

    private void schedulePending() {
        UiRuntime currentRuntime = runtime();
        if (currentRuntime == null) {
            detachedScheduler.schedule(this, this::applyPending);
            return;
        }
        schedulerFor(currentRuntime).schedule(this, () -> {
            if (runtime() == currentRuntime) applyPending();
        });
    }

    private void ensureBuilt() {
        UiRuntime currentRuntime = runtime();
        if (currentRuntime == null) detachedScheduler.flush();
        else schedulerFor(currentRuntime).flush();
    }

    private static FrameScheduler schedulerFor(UiRuntime runtime) {
        synchronized (RUNTIME_SCHEDULERS) {
            return RUNTIME_SCHEDULERS.computeIfAbsent(runtime, ignored -> new FrameScheduler());
        }
    }

    private void applyPending() {
        if (runtime() == null) return;
        long started = profiler.begin();
        tree.reconcile(this, pending);
        markBuilt();
        profiler.record(this, UiProfiler.Phase.RECONCILE, started, "description tree changed");
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
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        long started = profiler.begin();
        renderChildren(g, font, mx, my, pt);
        profiler.record(this, UiProfiler.Phase.PAINT, started, null);
    }
}
