package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class Chip extends UIComponent {
    private final Component label;
    private IconWidget icon;
    private Runnable onRemove;
    private boolean selected;

    public Chip(String label) {
        this(Component.literal(label != null ? label : ""));
    }

    public Chip(Component label) {
        this.label = Objects.requireNonNull(label);
        focusable(true);
    }

    public Chip icon(IconWidget icon) {
        this.icon = icon;
        invalidateLayout();
        return this;
    }

    public Chip onRemove(Runnable onRemove) {
        this.onRemove = onRemove;
        invalidateLayout();
        return this;
    }

    public Chip selected(boolean selected) {
        this.selected = selected;
        invalidatePaint();
        return this;
    }

    @Override
    public int preferredWidth(Font font) {
        int w = font != null ? font.width(label) + 12 : 30;
        if (icon != null) w += 14;
        if (onRemove != null) w += 12;
        return w;
    }

    @Override
    public int preferredHeight(Font font) {
        return 16;
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int bg = selected ? colors.primaryDim() : (isHovered() ? colors.surfaceVariant() : colors.surfaceRaised());
        int border = selected ? colors.primary() : colors.border();

        UiRender.roundedOutline(g, x, y, width, height, height / 2, bg, border);

        int curX = x + 6;
        if (icon != null) {
            icon.layout(curX, y + (height - 10) / 2, 10, 10);
            icon.render(g, font, mx, my, pt);
            curX += 12;
        }

        int textCol = selected ? colors.onPrimary() : colors.onSurface();
        g.text(font, label, curX, y + (height - font.lineHeight) / 2, textCol);
        curX += font.width(label);

        if (onRemove != null) {
            int removeX = x + width - 12;
            int removeY = y + (height - font.lineHeight) / 2;
            boolean removeHovered = mx >= removeX - 2 && mx < removeX + 10 && my >= y && my < y + height;
            int removeCol = selected ? colors.onPrimary() : (removeHovered ? colors.danger() : colors.onSurfaceMuted());
            g.text(font, "×", removeX, removeY, removeCol);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && isHovered()) {
            if (onRemove != null && mx >= x + width - 14) {
                onRemove.run();
                return true;
            }
        }
        return false;
    }
}
