package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.ReadableSignal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class ProgressBar extends UIComponent {
    private final ReadableSignal<? extends Number> progress;
    public ProgressBar(ReadableSignal<? extends Number> progress) { this.progress = progress; }
    @Override public int preferredWidth(Font font) { return 80; }
    @Override public int preferredHeight(Font font) { return 8; }
    @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        float value = Math.max(0f, Math.min(1f, progress.get().floatValue()));
        var colors = theme().colors();
        UiRender.roundedRect(g, x, y, width, height, height / 2, colors.input());
        UiRender.roundedRect(g, x, y, Math.round(width * value), height, height / 2, colors.primary());
    }
}

