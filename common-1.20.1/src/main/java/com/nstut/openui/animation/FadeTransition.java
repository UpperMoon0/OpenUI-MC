package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class FadeTransition extends Transition {

    public FadeTransition(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible || progress <= 0.001F) return;
        child.render(g, font, mx, my, pt);
    }
}
