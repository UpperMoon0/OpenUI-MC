package com.nstut.openui.debug;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.DeclarativeHost;
import com.nstut.openui.api.ScopedUIComponent;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.component.DirtyFlag;
import com.nstut.openui.declarative.DeclarativeTree;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.semantics.SemanticNarration;
import com.nstut.openui.state.Signals;
import com.nstut.openui.state.UiScope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UiInspector extends UIComponent {
    private final UiRuntime runtime;
    private boolean showOutlines = true;
    private boolean showTree = true;
    private UIComponent selectedComponent;
    private OverlayHandle handle;
    private int treeScrollOffset = 0;

    public UiInspector(UiRuntime runtime) {
        this.runtime = runtime;
        focusable(false);
    }

    public static OverlayHandle toggle(UiRuntime runtime) {
        if (runtime == null) return null;
        for (UIComponent c : runtime.overlays().components()) {
            if (c instanceof UiInspector insp) {
                if (insp.handle != null) insp.handle.close();
                return null;
            }
        }
        UiInspector inspector = new UiInspector(runtime);
        inspector.setProfiling(true);
        inspector.handle = runtime.overlays().show(
                OverlayLayer.DEBUG,
                inspector,
                false,
                true,
                false,
                () -> {
                    inspector.setProfiling(false);
                    inspector.handle = null;
                });
        return inspector.handle;
    }

    @Override
    public int preferredWidth(Font font) { return runtime.root() != null ? runtime.root().getWidth() : 200; }

    @Override
    public int preferredHeight(Font font) { return runtime.root() != null ? runtime.root().getHeight() : 200; }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(0, 0, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (runtime == null || runtime.root() == null) return;
        setProfiling(true);

        if (showOutlines) {
            renderOutlines(g, runtime.root(), mx, my);
            for (UIComponent overlay : runtime.overlays().components()) {
                if (overlay != this) renderOutlines(g, overlay, mx, my);
            }
        }
        if (showTree) renderInfoPanel(g, font, mx, my);
    }

    private void renderOutlines(GuiGraphicsExtractor g, UIComponent comp, int mx, int my) {
        if (comp == null || !comp.isVisible()) return;
        int cx = comp.getX();
        int cy = comp.getY();
        int cw = comp.getWidth();
        int ch = comp.getHeight();
        boolean selected = comp == selectedComponent;
        int outlineColor = selected ? 0xFFFF00FF
                : (comp.isFocused() ? 0xFF00FF00 : (comp.isHovered() ? 0xFF00FFFF : 0x403388FF));
        g.fill(cx, cy, cx + cw, cy + 1, outlineColor);
        g.fill(cx, cy + ch - 1, cx + cw, cy + ch, outlineColor);
        g.fill(cx, cy, cx + 1, cy + ch, outlineColor);
        g.fill(cx + cw - 1, cy, cx + cw, cy + ch, outlineColor);
        for (UIComponent child : comp.children()) renderOutlines(g, child, mx, my);
    }

    private void renderInfoPanel(GuiGraphicsExtractor g, Font font, int mx, int my) {
        int panelW = 286;
        int panelH = Math.min(318, Math.max(140, height - 16));
        int panelX = Math.max(8, width - panelW - 8);
        int panelY = 8;
        UiRender.roundedOutline(g, panelX, panelY, panelW, panelH, 4, 0xE0101016, 0xFF3D3D4E);

        int textH = font != null ? font.lineHeight : 9;
        if (font == null) return;
        g.text(font, "UI Inspector / Devtools", panelX + 6, panelY + 6, 0xFF5B8DF2, false);
        g.text(font,
                "Mounted: " + countMounted(runtime.root()) + "  Anim: " + runtime.animations().activeCount() + "  Overlays: " + runtime.overlays().size(),
                panelX + 6, panelY + 18, 0xFF8A8A9E, false);

        UIComponent target = selectedComponent != null ? selectedComponent : runtime.root().hitTest(mx, my);
        if (target == null) return;

        int y = panelY + 32;
        String name = target.getClass().getSimpleName();
        if (name.isEmpty()) name = target.getClass().getName();
        drawLine(g, font, panelX, y, "Target: " + name + (target.key() != null ? " [" + target.key() + "]" : ""), 0xFFE2E2EA); y += 12;
        drawLine(g, font, panelX, y, "Bounds: " + target.getX() + "," + target.getY() + " " + target.getWidth() + "x" + target.getHeight(), 0xFF8A8A9E); y += 12;
        drawLine(g, font, panelX, y, "State: vis=" + target.isVisible() + " hov=" + target.isHovered() + " foc=" + target.isFocused(), 0xFF8A8A9E); y += 12;
        drawLine(g, font, panelX, y, "Dirty: B=" + target.isDirty(DirtyFlag.BUILD) + " L=" + target.isDirty(DirtyFlag.LAYOUT) + " P=" + target.isDirty(DirtyFlag.PAINT), 0xFFE5A43B); y += 12;

        ScopedUIComponent scoped = nearest(target, ScopedUIComponent.class);
        UiScope.DebugSnapshot scope = scoped != null ? scoped.scopeDebugSnapshot().orElse(null) : null;
        if (scope != null) {
            drawLine(g, font, panelX, y,
                    "Scope: effects=" + scope.effects() + " subs=" + scope.subscriptions() + " async=" + scope.asyncTasks()
                            + " resources=" + scope.resources(), 0xFF8A8A9E); y += 12;
            drawLine(g, font, panelX, y,
                    "State slots=" + scope.rememberedState() + " keyed=" + scope.keyedResources() + " signals=" + scope.signals().size(),
                    0xFF8A8A9E); y += 12;
            if (!scope.signals().isEmpty()) {
                Signals.DebugSignal signal = scope.signals().get(0);
                String deps = signal.dependencies().isEmpty() ? "" : " <- " + String.join(",", signal.dependencies());
                drawLine(g, font, panelX, y, "Signal: " + signal.displayName() + deps, 0xFF60A5FA); y += 12;
            }
        }

        DeclarativeHost host = nearest(target, DeclarativeHost.class);
        if (host != null) {
            DeclarativeTree.Diagnostics d = host.reconcileDiagnostics();
            drawLine(g, font, panelX, y,
                    "Reconcile: +" + d.created() + " =" + d.reused() + " -" + d.removed() + " managed=" + d.managedNodes(),
                    0xFF60A5FA); y += 12;

            UiProfiler.Snapshot stats = host.profiler().snapshot().get(host);
            if (stats != null) {
                drawLine(g, font, panelX, y,
                        "Profile: updates=" + stats.updates() + " build=" + time(stats.buildNanos())
                                + " layout=" + time(stats.layoutNanos()) + " paint=" + time(stats.paintNanos()),
                        0xFF8A8A9E); y += 12;
                if (stats.lastCause() != null) {
                    drawLine(g, font, panelX, y, "Why: " + stats.lastCause(), 0xFFE5A43B); y += 12;
                }
            }

            UiProfiler.TraceEntry event = latestEvent(host.profiler());
            if (event != null) {
                drawLine(g, font, panelX, y,
                        "Event: " + (event.cause() != null ? event.cause() : event.phase()) + " " + time(event.durationNanos()),
                        0xFF8A8A9E); y += 12;
            }
            int warnings = host.profiler().warnings().size();
            if (warnings > 0) {
                drawLine(g, font, panelX, y, "Slow warnings: " + warnings, 0xFFF87171); y += 12;
            }
        }

        String narration = SemanticNarration.describeNearest(target);
        if (!narration.isBlank()) {
            drawLine(g, font, panelX, y, "Semantics: " + narration, 0xFF4ADE80); y += 12;
        }

        y += 2;
        drawLine(g, font, panelX, y, "── Component Hierarchy ──", 0xFF6B7280); y += 12;
        List<TreeEntry> entries = new ArrayList<>();
        buildTree(runtime.root(), 0, entries);
        int treeY = y;
        int availableTreeHeight = Math.max(0, panelY + panelH - treeY - 6);
        int maxTreeLines = Math.max(1, availableTreeHeight / (textH + 2));
        int startIdx = Math.max(0, Math.min(treeScrollOffset, Math.max(0, entries.size() - maxTreeLines)));

        ClipStack.push(g, panelX + 4, treeY, panelW - 8, availableTreeHeight);
        try {
            for (int i = startIdx; i < Math.min(entries.size(), startIdx + maxTreeLines); i++) {
                TreeEntry entry = entries.get(i);
                int rowY = treeY + (i - startIdx) * (textH + 2);
                int col = entry.component == target ? 0xFF5B8DF2
                        : (entry.component.isFocused() ? 0xFF4ADE80 : 0xFF9CA3AF);
                String indent = "  ".repeat(entry.depth);
                String line = indent + (entry.component.children().isEmpty() ? "• " : "▼ ") + entry.label;
                g.text(font, trim(line, 47), panelX + 6, rowY, col, false);
            }
        } finally {
            ClipStack.pop(g);
        }
    }

    private static UiProfiler.TraceEntry latestEvent(UiProfiler profiler) {
        List<UiProfiler.TraceEntry> trace = profiler.trace();
        for (int i = trace.size() - 1; i >= 0; i--) {
            if (trace.get(i).phase() == UiProfiler.Phase.EVENT) return trace.get(i);
        }
        return null;
    }

    private static String time(long nanos) {
        if (nanos < 1_000) return nanos + "ns";
        if (nanos < 1_000_000) return String.format(Locale.ROOT, "%.1fus", nanos / 1_000.0);
        return String.format(Locale.ROOT, "%.2fms", nanos / 1_000_000.0);
    }

    private static void drawLine(GuiGraphicsExtractor g, Font font, int panelX, int y, String text, int color) {
        g.text(font, trim(text, 47), panelX + 6, y, color, false);
    }

    private static String trim(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static <T extends UIComponent> T nearest(UIComponent component, Class<T> type) {
        UIComponent cursor = component;
        while (cursor != null) {
            if (type.isInstance(cursor)) return type.cast(cursor);
            cursor = cursor.parent();
        }
        return null;
    }

    private record TreeEntry(UIComponent component, int depth, String label) { }

    private void buildTree(UIComponent comp, int depth, List<TreeEntry> output) {
        if (comp == null) return;
        String name = comp.getClass().getSimpleName();
        if (name.isEmpty()) name = comp.getClass().getName();
        if (comp.key() != null) name += " (" + comp.key() + ")";
        output.add(new TreeEntry(comp, depth, name));
        for (UIComponent child : comp.children()) buildTree(child, depth + 1, output);
    }

    private int countMounted(UIComponent comp) {
        if (comp == null) return 0;
        int total = 1;
        for (UIComponent child : comp.children()) total += countMounted(child);
        return total;
    }

    private void setProfiling(boolean enabled) {
        setProfiling(runtime.root(), enabled);
        for (UIComponent overlay : runtime.overlays().components()) {
            if (overlay != this) setProfiling(overlay, enabled);
        }
    }

    private void setProfiling(UIComponent component, boolean enabled) {
        if (component == null) return;
        if (component instanceof DeclarativeHost host) host.profiler().enabled(enabled);
        for (UIComponent child : component.children()) setProfiling(child, enabled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta != 0) {
            treeScrollOffset = Math.max(0, treeScrollOffset - (int) Math.signum(delta));
            invalidatePaint();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && runtime != null && runtime.root() != null) {
            selectedComponent = runtime.root().hitTest((int) mx, (int) my);
            invalidatePaint();
            return false;
        }
        return false;
    }
}
