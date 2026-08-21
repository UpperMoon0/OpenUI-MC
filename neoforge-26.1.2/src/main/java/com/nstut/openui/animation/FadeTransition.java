package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class FadeTransition extends Transition {

    public FadeTransition(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (progress <= 0.001F) return;
        if (progress >= 0.999F) {
            child.render(g, font, mx, my, pt);
            return;
        }
        // 26.1's extracted GUI renderer no longer exposes mutable global shader color.
        child.render(g, font, mx, my, pt);
    }
}
