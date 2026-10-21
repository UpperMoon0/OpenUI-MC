package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ScaleTransition extends Transition {

    public ScaleTransition(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible || progress <= 0.001F) return;

        float s = Math.max(0.0F, progress);
        if (Math.abs(s - 1.0F) < 0.001F) {
            child.render(g, font, mx, my, pt);
            return;
        }

        int cx = x + width / 2;
        int cy = y + height / 2;

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(s, s);
        g.pose().translate(-cx, -cy);
        try {
            child.render(g, font, mx, my, pt);
        } finally {
            g.pose().popMatrix();
        }
    }
}
