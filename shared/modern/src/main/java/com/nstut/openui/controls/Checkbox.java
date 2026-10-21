package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Subscription;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Checkbox extends UIComponent {
    private final Signal<Boolean> checked;
    private final String label;
    private Subscription subscription = Subscription.EMPTY;

    public Checkbox(String label, Signal<Boolean> checked) { this.label = label; this.checked = checked; focusable(true); }
    @Override public int preferredWidth(Font font) { return 14 + (font == null ? 30 : font.width(label)); }
    @Override public int preferredHeight(Font font) { return 12; }
    @Override protected void onMount() { subscription = checked.subscribe(ignored -> invalidatePaint()); }
    @Override protected void onUnmount() { subscription.close(); subscription = Subscription.EMPTY; }

    @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        var colors = theme().colors();
        UiRender.roundedOutline(g, x, y, 10, 10, 2, checked.get() ? colors.primary() : colors.input(), isFocused() ? colors.primary() : colors.border());
        if (checked.get()) {
            g.fill(x + 2, y + 4, x + 4, y + 7, colors.onPrimary());
            g.fill(x + 4, y + 6, x + 8, y + 8, colors.onPrimary());
        }
        g.text(font, label, x + 14, y + 1, colors.onSurface(), false);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && mx >= x && mx < x + width && my >= y && my < y + height) { checked.set(!checked.get()); return true; }
        return false;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 257 || key == 32) { checked.set(!checked.get()); return true; }
        return false;
    }
}

