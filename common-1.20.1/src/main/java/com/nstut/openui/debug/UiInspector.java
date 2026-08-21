package com.nstut.openui.debug;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
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
        int panelW = 210;
        int panelH = 140;
        int panelX = width - panelW - 8;
        int panelY = 8;

        UiRender.roundedOutline(g, panelX, panelY, panelW, panelH, 4, 0xE0101016, 0xFF3D3D4E);

        g.drawString(font, "UI Inspector (Debug)", panelX + 6, panelY + 6, 0xFF5B8DF2);

        int count = countMounted(runtime.root());
        int activeAnim = runtime.animations().activeCount();
        int activeOverlays = runtime.overlays().size();

        g.drawString(font, "Mounted: " + count + "  Anim: " + activeAnim + "  Overlays: " + activeOverlays, panelX + 6, panelY + 20, 0xFF8A8A9E);

        UIComponent target = selectedComponent != null ? selectedComponent : runtime.root().hitTest(mx, my);
        if (target != null) {
            String name = target.getClass().getSimpleName();
            if (name.isEmpty()) name = target.getClass().getName();
            g.drawString(font, "Target: " + name, panelX + 6, panelY + 36, 0xFFE2E2EA);
            g.drawString(font, "Bounds: [" + target.getX() + ", " + target.getY() + ", " + target.getWidth() + "x" + target.getHeight() + "]", panelX + 6, panelY + 48, 0xFF8A8A9E);
            g.drawString(font, "Focusable: " + target.isFocusable() + "  Focused: " + target.isFocused(), panelX + 6, panelY + 60, 0xFF8A8A9E);
            g.drawString(font, "Visible: " + target.isVisible() + "  Hovered: " + target.isHovered(), panelX + 6, panelY + 72, 0xFF8A8A9E);
            if (target.key() != null) {
                g.drawString(font, "Key: " + target.key(), panelX + 6, panelY + 84, 0xFFE5A43B);
            }
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
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            selectedComponent = runtime.root().hitTest((int) mx, (int) my);
            return false;
        }
        return false;
    }
}
