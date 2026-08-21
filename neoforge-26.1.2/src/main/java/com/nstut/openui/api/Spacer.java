package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Spacer extends UIComponent {
    @Override public int preferredWidth(Font font) { return 0; }
    @Override public int preferredHeight(Font font) { return 0; }
    @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {}
}
