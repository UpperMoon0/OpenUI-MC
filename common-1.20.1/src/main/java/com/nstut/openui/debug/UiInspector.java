package com.nstut.openui.debug;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.component.DirtyFlag;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

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
        inspector.handle = runtime.overlays().show(OverlayLayer.DEBUG, inspector, false, true, false, () -> inspector.handle = null);
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
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (runtime == null || runtime.root() == null) return;

        if (showOutlines) {
            renderOutlines(g, runtime.root(), mx, my);
            for (UIComponent overlay : runtime.overlays().components()) {
                if (overlay != this) renderOutlines(g, overlay, mx, my);
            }
        }

        if (showTree) {
            renderInfoPanel(g, font, mx, my);
        }
    }

    private void renderOutlines(GuiGraphics g, UIComponent comp, int mx, int my) {
        if (comp == null || !comp.isVisible()) return;

        int cx = comp.getX();
        int cy = comp.getY();
        int cw = comp.getWidth();
        int ch = comp.getHeight();

        boolean hovered = comp.isHovered();
        boolean focused = comp.isFocused();
        boolean selected = (comp == selectedComponent);

        int outlineColor = selected ? 0xFFFF00FF : (focused ? 0xFF00FF00 : (hovered ? 0xFF00FFFF : 0x403388FF));

        // Draw component border
        g.fill(cx, cy, cx + cw, cy + 1, outlineColor);
        g.fill(cx, cy + ch - 1, cx + cw, cy + ch, outlineColor);
        g.fill(cx, cy, cx + 1, cy + ch, outlineColor);
        g.fill(cx + cw - 1, cy, cx + cw, cy + ch, outlineColor);

        for (UIComponent child : comp.children()) {
            renderOutlines(g, child, mx, my);
        }
    }

    private void renderInfoPanel(GuiGraphics g, Font font, int mx, int my) {
        int panelW = 240;
        int panelH = Math.min(220, height - 16);
        int panelX = width - panelW - 8;
        int panelY = 8;

        UiRender.roundedOutline(g, panelX, panelY, panelW, panelH, 4, 0xE0101016, 0xFF3D3D4E);

        int textH = font != null ? font.lineHeight : 9;
        if (font != null) {
            UiRender.text(g, font, "UI Inspector", panelX + 6, panelY + 6, 0xFF5B8DF2);
        }

        int count = countMounted(runtime.root());
        int activeAnim = runtime.animations().activeCount();
        int activeOverlays = runtime.overlays().size();

        if (font != null) {
            UiRender.text(g, font, "Mounted: " + count + "  Anim: " + activeAnim + "  Overlays: " + activeOverlays, panelX + 6, panelY + 18, 0xFF8A8A9E);
        }

        UIComponent target = selectedComponent != null ? selectedComponent : runtime.root().hitTest(mx, my);
        if (target != null && font != null) {
            String name = target.getClass().getSimpleName();
            if (name.isEmpty()) name = target.getClass().getName();
            UiRender.text(g, font, "Target: " + name + (target.key() != null ? " [" + target.key() + "]" : ""), panelX + 6, panelY + 32, 0xFFE2E2EA);
            UiRender.text(g, font, "Bounds: " + target.getX() + "," + target.getY() + " " + target.getWidth() + "x" + target.getHeight(), panelX + 6, panelY + 44, 0xFF8A8A9E);
            UiRender.text(g, font, "State: vis=" + target.isVisible() + " hov=" + target.isHovered() + " foc=" + target.isFocused(), panelX + 6, panelY + 56, 0xFF8A8A9E);
            UiRender.text(g, font, "Dirty: B=" + target.isDirty(DirtyFlag.BUILD) + " L=" + target.isDirty(DirtyFlag.LAYOUT) + " P=" + target.isDirty(DirtyFlag.PAINT), panelX + 6, panelY + 68, 0xFFE5A43B);
            UiRender.text(g, font, "Flex: " + target.isFlex() + " grow=" + target.getFlexGrow(), panelX + 6, panelY + 80, 0xFF8A8A9E);

            // Render mini tree
            UiRender.text(g, font, "── Component Hierarchy ──", panelX + 6, panelY + 96, 0xFF6B7280);
            List<TreeEntry> entries = new ArrayList<>();
            buildTree(runtime.root(), 0, entries);

            int treeY = panelY + 108;
            int maxTreeLines = Math.max(1, (panelH - 114) / (textH + 2));
            int startIdx = Math.max(0, Math.min(treeScrollOffset, Math.max(0, entries.size() - maxTreeLines)));

            ClipStack.push(g, panelX + 4, treeY, panelW - 8, panelH - 112);
            try {
                for (int i = startIdx; i < Math.min(entries.size(), startIdx + maxTreeLines); i++) {
                    TreeEntry entry = entries.get(i);
                    int rowY = treeY + (i - startIdx) * (textH + 2);
                    boolean isTarget = (entry.component == target);
                    int col = isTarget ? 0xFF5B8DF2 : (entry.component.isFocused() ? 0xFF4ADE80 : 0xFF9CA3AF);
                    String indent = "  ".repeat(entry.depth);
                    String line = indent + (entry.component.children().isEmpty() ? "• " : "▼ ") + entry.label;
                    UiRender.text(g, font, line, panelX + 6, rowY, col);
                }
            } finally {
                ClipStack.pop(g);
            }
        }
    }

    private record TreeEntry(UIComponent component, int depth, String label) {}

    private void buildTree(UIComponent comp, int depth, List<TreeEntry> output) {
        if (comp == null) return;
        String name = comp.getClass().getSimpleName();
        if (name.isEmpty()) name = comp.getClass().getName();
        if (comp.key() != null) name += " (" + comp.key() + ")";
        output.add(new TreeEntry(comp, depth, name));
        for (UIComponent child : comp.children()) {
            buildTree(child, depth + 1, output);
        }
    }

    private int countMounted(UIComponent comp) {
        if (comp == null) return 0;
        int total = 1;
        for (UIComponent child : comp.children()) {
            total += countMounted(child);
        }
        return total;
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
