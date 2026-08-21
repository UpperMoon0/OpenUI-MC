package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.Signal;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class Radio<T> extends UIComponent {
    private final T value;
    private final Component label;
    private final Signal<T> signal;
    private boolean enabled = true;

    public Radio(T value, String label, Signal<T> signal) {
        this(value, Component.literal(label), signal);
    }

    public Radio(T value, Component label, Signal<T> signal) {
        this.value = value;
        this.label = Objects.requireNonNull(label);
        this.signal = Objects.requireNonNull(signal);
        focusable(true);
    }

    public Radio<T> enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isSelected() {
        return Objects.equals(signal.get(), value);
    }

    public void select() {
        if (enabled) {
            signal.set(value);
            invalidatePaint();
        }
    }

    @Override
    public int preferredWidth(Font font) {
        return font != null ? font.width(label) + 20 : 60;
    }

    @Override
    public int preferredHeight(Font font) {
        return 14;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int radioRadius = 5;
        int radioX = x + 6;
        int radioY = y + height / 2;

        boolean selected = isSelected();
        int bg = isHovered() ? colors.surfaceVariant() : colors.surfaceRaised();
        int border = isFocused() ? colors.primary() : (selected ? colors.primary() : colors.border());

        // Outer circle
        UiRender.roundedOutline(g, radioX - radioRadius, radioY - radioRadius, radioRadius * 2, radioRadius * 2, radioRadius, bg, border);

        // Inner dot
        if (selected) {
            int innerR = 2;
            UiRender.roundedRect(g, radioX - innerR, radioY - innerR, innerR * 2, innerR * 2, innerR, colors.primary());
        }

        int textCol = enabled ? (isHovered() ? colors.onSurface() : colors.onSurfaceMuted()) : colors.onSurfaceDisabled();
        g.drawString(font, label, x + 16, y + (height - font.lineHeight) / 2, textCol);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && enabled && isHovered()) {
            select();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if ((key == 257 || key == 32) && enabled && isFocused()) {
            select();
            return true;
        }
        return false;
    }
}
