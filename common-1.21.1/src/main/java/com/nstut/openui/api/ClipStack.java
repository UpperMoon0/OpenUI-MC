package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ClipStack extends Stack {
    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            super.render(graphics, font, mouseX, mouseY, partialTick);
        } finally {
            graphics.disableScissor();
        }
    }
}

