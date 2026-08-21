package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ScaleTransition extends Transition {

    public ScaleTransition(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible || progress <= 0.001F) return;

        float s = Math.max(0.0F, progress);
        if (Math.abs(s - 1.0F) < 0.001F) {
            child.render(g, font, mx, my, pt);
            return;
        }

        int cx = x + width / 2;
        int cy = y + height / 2;

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(s, s, 1.0F);
        g.pose().translate(-cx, -cy, 0);
        try {
            child.render(g, font, mx, my, pt);
        } finally {
            g.pose().popPose();
        }
    }
}
