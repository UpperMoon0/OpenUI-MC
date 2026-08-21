package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.api.ClipStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A transition that fades its child in/out by alpha-blending a solid colour
 * overlay: at progress=0 the child is invisible; at progress=1 it is fully
 * visible.  GuiGraphics does not natively expose an alpha multiplier that works
 * for all draw calls, so the technique is:
 *   - render the child to the screen normally,
 *   - then overdraw a black rect at (1-progress) opacity to simulate fade.
 * This gives a correct black-fade-in/out effect without requiring PoseStack
 * matrix manipulation or render targets.
 */
public class FadeTransition extends Transition {

    public FadeTransition(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (progress <= 0.001F) return; // fully invisible
        if (progress >= 0.999F) {
            // Fully visible – just render the child directly
            child.render(g, font, mx, my, pt);
            return;
        }
        // Render child then apply an inverse-alpha overlay
        child.render(g, font, mx, my, pt);
        int alpha = (int) ((1.0F - progress) * 255) & 0xFF;
        // Background colour to fade from – use opaque black
        int overlay = (alpha << 24) | 0x000000;
        g.fill(x, y, x + width, y + height, overlay);
    }
}
