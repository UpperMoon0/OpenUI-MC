package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

public class Popover extends UIComponent {
    public enum Placement { TOP, BOTTOM, LEFT, RIGHT, AUTO }

    private final UIComponent anchor;
    private final UIComponent content;
    private Placement placement = Placement.AUTO;
    private int offset = 4;
    private OverlayHandle handle;

    public Popover(UIComponent anchor, UIComponent content) {
        this.anchor = Objects.requireNonNull(anchor);
        this.content = Objects.requireNonNull(content);
        addChild(content);
    }

    public Popover placement(Placement placement) {
        this.placement = Objects.requireNonNull(placement);
        invalidateLayout();
        return this;
    }

    public Popover offset(int offset) {
        this.offset = offset;
        invalidateLayout();
        return this;
    }

    public OverlayHandle show(com.nstut.openui.overlay.OverlayManager overlays) {
        if (overlays == null) return null;
        handle = overlays.show(
                OverlayLayer.POPOVER,
                this,
                false,
                true,
                true,
                () -> handle = null
        );
        return handle;
    }

    public void close() {
        if (handle != null) {
            handle.close();
            handle = null;
        }
    }

    @Override
    public int preferredWidth(Font font) {
        return content.preferredWidth(font) + 12;
    }

    @Override
    public int preferredHeight(Font font) {
        return content.preferredHeight(font) + 12;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        Font font = measureFont();
        int contentW = content.preferredWidth(font);
        int contentH = content.preferredHeight(font);
        int popoverW = contentW + 12;
        int popoverH = contentH + 12;

        int ax = anchor.getX();
        int ay = anchor.getY();
        int aw = anchor.getWidth();
        int ah = anchor.getHeight();

        int targetX = ax;
        int targetY = ay + ah + offset;

        Placement effectivePlacement = placement;
        if (effectivePlacement == Placement.AUTO) {
            if (ay + ah + offset + popoverH > availableHeight && ay - offset - popoverH >= 0) {
                effectivePlacement = Placement.TOP;
            } else {
                effectivePlacement = Placement.BOTTOM;
            }
        }

        switch (effectivePlacement) {
            case TOP -> {
                targetX = ax + (aw - popoverW) / 2;
                targetY = ay - popoverH - offset;
            }
            case BOTTOM -> {
                targetX = ax + (aw - popoverW) / 2;
                targetY = ay + ah + offset;
            }
            case LEFT -> {
                targetX = ax - popoverW - offset;
                targetY = ay + (ah - popoverH) / 2;
            }
            case RIGHT -> {
                targetX = ax + aw + offset;
                targetY = ay + (ah - popoverH) / 2;
            }
            case AUTO -> {}
        }

        // Viewport clamping
        targetX = Math.max(4, Math.min(availableWidth - popoverW - 4, targetX));
        targetY = Math.max(4, Math.min(availableHeight - popoverH - 4, targetY));

        setBounds(targetX, targetY, popoverW, popoverH);
        content.layoutTree(font, targetX + 6, targetY + 6, contentW, contentH);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.cardTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.cardTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

        content.render(g, font, mx, my, pt);
    }
}
