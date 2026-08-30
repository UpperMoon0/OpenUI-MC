package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

public class Popover extends UIComponent {
    public enum Placement { TOP, BOTTOM, LEFT, RIGHT, AUTO }

    private final UIComponent anchor;
    private final UIComponent content;
    private Placement placement = Placement.AUTO;
    private int offset = 4;
    private boolean matchAnchorWidth;
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

    public Popover matchAnchorWidth() {
        this.matchAnchorWidth = true;
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
        int anchorWidth = anchor.getWidth();
        int desiredPopoverW = matchAnchorWidth ? anchorWidth : content.preferredWidth(font) + 12;
        int popoverW = Math.min(Math.max(12, desiredPopoverW), Math.max(12, availableWidth - 8));
        int contentW = Math.max(0, popoverW - 12);
        int maxPopoverH = Math.max(0, availableHeight - 8);
        int maxContentH = Math.max(0, maxPopoverH - 12);
        Size contentSize = content.measure(Constraints.loose(contentW, maxContentH), font);
        int contentH = contentSize.height();
        int popoverH = Math.min(maxPopoverH, contentH + 12);

        int ax = anchor.getX();
        int ay = anchor.getY();
        int aw = anchor.getWidth();
        int ah = anchor.getHeight();

        int targetX = ax;
        int targetY = ay + ah + offset;

        Placement effectivePlacement = placement;
        if (effectivePlacement == Placement.AUTO) {
            if (ay + ah + offset + popoverH > ly + availableHeight && ay - offset - popoverH >= ly) {
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

        // Viewport clamping with origin offset
        int minX = lx + 4;
        int maxX = lx + Math.max(0, availableWidth - popoverW - 4);
        int minY = ly + 4;
        int maxY = ly + Math.max(0, availableHeight - popoverH - 4);

        targetX = Math.max(minX, Math.min(maxX, targetX));
        targetY = Math.max(minY, Math.min(maxY, targetY));

        setBounds(targetX, targetY, popoverW, popoverH);
        content.layoutTree(font, targetX + 6, targetY + 6, contentW, contentH);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.cardTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.cardTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

        content.render(g, font, mx, my, pt);
    }
}
