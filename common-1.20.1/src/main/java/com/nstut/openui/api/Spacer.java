package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class Spacer extends UIComponent {
    private int preferredWidth;
    private int preferredHeight;

    @Override public Spacer width(int value) {
        preferredWidth = Math.max(0, value);
        super.width(value);
        return this;
    }

    @Override public Spacer height(int value) {
        preferredHeight = Math.max(0, value);
        super.height(value);
        return this;
    }

    @Override public int preferredWidth(Font font) { return preferredWidth; }
    @Override public int preferredHeight(Font font) { return preferredHeight; }
    @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {}
}
