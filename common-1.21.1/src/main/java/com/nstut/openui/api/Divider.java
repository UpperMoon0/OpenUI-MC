package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class Divider extends UIComponent {

    private final int color;

    public Divider() { this(0); }
    public Divider(int color) { this.color = color; }

    @Override
    public int preferredWidth(Font font) { return 0; }

    @Override
    public int preferredHeight(Font font) { return 1; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        int col = color != 0 ? color : theme().colors().borderSubtle();
        g.fill(x, y, x + width, y + height, col);
    }
}
