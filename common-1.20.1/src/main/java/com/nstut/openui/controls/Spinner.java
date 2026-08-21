package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class Spinner extends UIComponent {
    private int size = 16;

    public Spinner() {
        this(16);
    }

    public Spinner(int size) {
        this.size = Math.max(8, size);
    }

    @Override
    public int preferredWidth(Font font) { return size; }

    @Override
    public int preferredHeight(Font font) { return size; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int cx = x + width / 2;
        int cy = y + height / 2;
        int r = Math.min(width, height) / 2 - 2;

        long time = System.currentTimeMillis();
        double angle = (time % 1000) / 1000.0 * Math.PI * 2;

        int ticks = 8;
        for (int i = 0; i < ticks; i++) {
            double tickAngle = angle + (i * Math.PI * 2 / ticks);
            float alpha = (float) (i + 1) / ticks;
            int tickColor = UiRender.mix(colors.surface(), colors.primary(), alpha);

            int px = cx + (int) Math.round(Math.cos(tickAngle) * r);
            int py = cy + (int) Math.round(Math.sin(tickAngle) * r);
            g.fill(px - 1, py - 1, px + 1, py + 1, tickColor);
        }
    }
}
