package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class Skeleton extends UIComponent {
    private int reqWidth = 60;
    private int reqHeight = 14;
    private int radius = 3;

    public Skeleton(int width, int height) {
        this.reqWidth = Math.max(4, width);
        this.reqHeight = Math.max(4, height);
    }

    public Skeleton radius(int radius) {
        this.radius = Math.max(0, radius);
        return this;
    }

    @Override
    public int preferredWidth(Font font) { return reqWidth; }

    @Override
    public int preferredHeight(Font font) { return reqHeight; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        float progress = (float) ((Math.sin(System.currentTimeMillis() / 300.0) + 1.0) / 2.0);
        int color = UiRender.mix(colors.surface(), colors.surfaceVariant(), progress);

        UiRender.roundedRect(g, x, y, width, height, radius, color);
    }
}
