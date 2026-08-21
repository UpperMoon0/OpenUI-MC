package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.input.EventType;
import com.nstut.openui.state.Signal;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Slider extends UIComponent {
    private final Signal<Double> value;
    private final double min, max;
    public Slider(Signal<Double> value, double min, double max) {
        this.value = value;
        this.min = min;
        this.max = Math.max(min, max);
        focusable(true);
        on(EventType.MOUSE_DOWN, event -> event.capturePointer());
    }
    @Override public int preferredWidth(Font font) { return 80; }
    @Override public int preferredHeight(Font font) { return 12; }
    @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        double progress = (value.get() - min) / Math.max(0.000001, max - min);
        progress = Math.max(0, Math.min(1, progress));
        var colors = theme().colors();
        g.fill(x, y + height / 2 - 1, x + width, y + height / 2 + 1, colors.border());
        int knobX = x + (int) Math.round(progress * width);
        UiRender.roundedRect(g, knobX - 3, y + height / 2 - 3, 6, 6, 3, colors.primary());
    }
    private void update(double mouseX) { double p = Math.max(0, Math.min(1, (mouseX - x) / Math.max(1, width))); value.set(min + p * (max - min)); }
    @Override public boolean mouseClicked(double mx, double my, int button) { if (button == 0 && mx >= x && mx <= x + width && my >= y && my <= y + height) { update(mx); return true; } return false; }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) { if (button == 0) { update(mx); return true; } return false; }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) { double step = (max - min) / 20d; if (key == 263) { value.set(Math.max(min, value.get() - step)); return true; } if (key == 262) { value.set(Math.min(max, value.get() + step)); return true; } return false; }
}
