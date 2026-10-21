package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Unsupported on Minecraft 26.1: extracted GUI rendering has no scoped opacity
 * operation that can faithfully fade an arbitrary component subtree.
 */
@Deprecated
public class FadeTransition extends Transition {

    public FadeTransition(UIComponent child) {
        super(child);
        throw new UnsupportedOperationException(
                "FadeTransition is unavailable on Minecraft 26.1; use SlideTransition or ScaleTransition");
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        throw new UnsupportedOperationException("FadeTransition is unavailable on Minecraft 26.1");
    }
}
