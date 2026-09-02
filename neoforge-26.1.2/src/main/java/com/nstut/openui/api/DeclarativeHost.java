package com.nstut.openui.api;

import com.nstut.openui.debug.UiProfiler;
import com.nstut.openui.declarative.DeclarativeChild;
import com.nstut.openui.declarative.KeyedReconciler;
import com.nstut.openui.declarative.NodeIdentity;
import com.nstut.openui.declarative.ReconcilePlan;
import com.nstut.openui.runtime.FrameScheduler;
import com.nstut.openui.state.UiScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Reactive declarative bridge backed by the retained 26.1.2 component tree. */
public final class DeclarativeHost extends ScopedUIComponent {
    private final Supplier<? extends List<DeclarativeChild<UIComponent>>> builder;
    private final FrameScheduler scheduler = new FrameScheduler();
    private final UiProfiler profiler = new UiProfiler();
    private List<DeclarativeChild<UIComponent>> descriptions = List.of();
    private List<DeclarativeChild<UIComponent>> pending = List.of();

    public DeclarativeHost(Supplier<? extends List<DeclarativeChild<UIComponent>>> builder) { this.builder = Objects.requireNonNull(builder, "builder"); }
    public UiProfiler profiler() { return profiler; }

    @Override
    protected void onScopedMount(UiScope scope) {
        scope.effect(() -> {
            long started = profiler.begin();
            List<DeclarativeChild<UIComponent>> next = builder.get();
            pending = next == null ? List.of() : List.copyOf(next);
            profiler.record(this, UiProfiler.Phase.BUILD, started, "reactive dependency changed");
            scheduler.schedule(this, this::applyPending);
            invalidateBuild();
        });
    }

    private void ensureBuilt() { scheduler.flush(); }

    private void applyPending() {
        long started = profiler.begin();
        List<DeclarativeChild<UIComponent>> nextDescriptions = pending;
        List<NodeIdentity> oldIds = descriptions.stream().map(DeclarativeChild::identity).toList();
        List<NodeIdentity> newIds = nextDescriptions.stream().map(DeclarativeChild::identity).toList();
        ReconcilePlan plan = KeyedReconciler.plan(oldIds, newIds);
        List<UIComponent> oldChildren = List.copyOf(children);
        List<UIComponent> nextChildren = new ArrayList<>(nextDescriptions.size());
        for (int i = 0; i < nextDescriptions.size(); i++) {
            DeclarativeChild<UIComponent> description = nextDescriptions.get(i);
            int oldIndex = plan.oldIndex(i);
            UIComponent component;
            if (oldIndex >= 0) { component = oldChildren.get(oldIndex); description.apply(component); }
            else { component = description.create(); addChild(component); }
            nextChildren.add(component);
        }
        for (int oldIndex : plan.removedOldIndices()) removeChild(oldChildren.get(oldIndex));
        children.clear();
        children.addAll(nextChildren);
        descriptions = nextDescriptions;
        profiler.record(this, UiProfiler.Phase.RECONCILE, started, "description tree changed");
        invalidateLayout();
    }

    @Override public int preferredWidth(Font font) { ensureBuilt(); int result = 0; for (UIComponent child : children) result = Math.max(result, child.preferredWidth(font)); return result; }
    @Override public int preferredHeight(Font font) { ensureBuilt(); int result = 0; for (UIComponent child : children) result = Math.max(result, child.preferredHeight(font)); return result; }

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
