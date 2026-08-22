package com.nstut.openui.controls;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.TextWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.TextStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class EmptyState extends UIComponent {
    private final Component title;
    private Component description;
    private IconWidget icon;
    private ButtonWidget actionButton;

    public EmptyState(String title) {
        this(Component.literal(title != null ? title : ""));
    }

    public EmptyState(Component title) {
        this.title = Objects.requireNonNull(title);
    }

    public EmptyState description(String desc) {
        this.description = Component.literal(desc != null ? desc : "");
        invalidateLayout();
        return this;
    }

    public EmptyState description(Component desc) {
        this.description = desc;
        invalidateLayout();
        return this;
    }

    public EmptyState icon(IconWidget icon) {
        this.icon = icon;
        invalidateLayout();
        return this;
    }

    public EmptyState action(String label, Runnable action) {
        this.actionButton = Ui.button(label, action).primary();
        addChild(this.actionButton);
        invalidateLayout();
        return this;
    }

    public EmptyState action(Component label, Runnable action) {
        this.actionButton = Ui.button(label, action).primary();
        addChild(this.actionButton);
        invalidateLayout();
        return this;
    }

    @Override
    public int preferredWidth(Font font) {
        return 180;
    }

    @Override
    public int preferredHeight(Font font) {
        return 120;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        if (actionButton != null) {
            Font f = measureFont();
            int bw = actionButton.preferredWidth(f);
            int bh = actionButton.preferredHeight(f);
            actionButton.layoutTree(f, lx + (availableWidth - bw) / 2, ly + availableHeight - bh - 10, bw, bh);
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        int centerY = y + height / 2 - 20;

        if (icon != null) {
            icon.layout(x + (width - 24) / 2, centerY - 28, 24, 24);
            icon.render(g, font, mx, my, pt);
        }

        int tw = font.width(title);
        UiRender.text(g, font, title, x + (width - tw) / 2, centerY, theme().colors().onSurface());

        if (description != null) {
            int dw = font.width(description);
            UiRender.text(g, font, description, x + (width - dw) / 2, centerY + font.lineHeight + 4, theme().colors().onSurfaceMuted());
        }

        if (actionButton != null) {
            actionButton.render(g, font, mx, my, pt);
        }
    }
}
