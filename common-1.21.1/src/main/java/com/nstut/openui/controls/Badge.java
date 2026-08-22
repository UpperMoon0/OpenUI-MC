package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class Badge extends UIComponent {
    public enum Variant { PRIMARY, SUCCESS, WARNING, DANGER, NEUTRAL }

    private final Component label;
    private Variant variant = Variant.PRIMARY;
    private boolean dotOnly;

    public Badge(String label) {
        this(Component.literal(label != null ? label : ""), Variant.PRIMARY);
    }

    public Badge(Component label, Variant variant) {
        this.label = Objects.requireNonNull(label);
        this.variant = Objects.requireNonNull(variant);
    }

    public static Badge dot(Variant variant) {
        Badge b = new Badge(Component.empty(), variant);
        b.dotOnly = true;
        return b;
    }

    public static Badge count(int count) {
        return new Badge(Component.literal(String.valueOf(count)), Variant.PRIMARY);
    }

    public Badge variant(Variant variant) {
        this.variant = Objects.requireNonNull(variant);
        invalidatePaint();
        return this;
    }

    @Override
    public int preferredWidth(Font font) {
        if (dotOnly) return 8;
        return font != null ? font.width(label) + 8 : 20;
    }

    @Override
    public int preferredHeight(Font font) {
        if (dotOnly) return 8;
        return 12;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int bg = switch (variant) {
            case PRIMARY -> colors.primary();
            case SUCCESS -> colors.success();
            case WARNING -> colors.warning();
            case DANGER -> colors.danger();
            case NEUTRAL -> colors.surfaceVariant();
        };

        if (dotOnly) {
            UiRender.roundedRect(g, x + (width - 6) / 2, y + (height - 6) / 2, 6, 6, 3, bg);
            return;
        }

        UiRender.roundedRect(g, x, y, width, height, height / 2, bg);
        int tw = font.width(label);
        g.drawString(font, label, x + (width - tw) / 2, y + (height - font.lineHeight) / 2, colors.onPrimary(), false);
    }
}
