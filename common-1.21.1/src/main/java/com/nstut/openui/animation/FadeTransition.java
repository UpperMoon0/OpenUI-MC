package com.nstut.openui.animation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class FadeTransition extends Transition {

    public FadeTransition(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (progress <= 0.001F) return;
        if (progress >= 0.999F) {
            child.render(g, font, mx, my, pt);
            return;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, progress);
        try {
            child.render(g, font, mx, my, pt);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
