package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.Signal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class SwitchControl extends UIComponent {
    private final Signal<Boolean> value;
    public SwitchControl(Signal<Boolean> value) { this.value = value; focusable(true); }
    @Override public int preferredWidth(Font font) { return 24; }
    @Override public int preferredHeight(Font font) { return 12; }
    @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        var colors = theme().colors();
        int track = value.get() ? colors.primary() : colors.input();
        UiRender.roundedOutline(g, x, y, width, height, height / 2, track, isFocused() ? colors.primary() : colors.border());
        int knob = Math.max(6, height - 4);
        int knobX = value.get() ? x + width - knob - 2 : x + 2;
        UiRender.roundedRect(g, knobX, y + 2, knob, knob, knob / 2, value.get() ? colors.onPrimary() : colors.onSurfaceMuted());
    }
    @Override public boolean mouseClicked(double mx, double my, int button) { if (button == 0 && mx >= x && mx < x + width && my >= y && my < y + height) { value.set(!value.get()); return true; } return false; }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) { if (key == 257 || key == 32) { value.set(!value.get()); return true; } return false; }
}

