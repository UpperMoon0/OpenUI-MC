package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public class Tooltip extends UIComponent {
    private final UIComponent content;
    private int mouseX, mouseY;

    public Tooltip(String text) {
        this(Component.literal(text != null ? text : ""));
    }

    public Tooltip(Component text) {
        this(new TooltipText(text));
    }

    public Tooltip(UIComponent content) {
        this.content = Objects.requireNonNull(content);
        addChild(content);
    }

    public void setPosition(int mx, int my) {
        this.mouseX = mx;
        this.mouseY = my;
        invalidateLayout();
    }

    @Override
    public int preferredWidth(Font font) {
        return content.preferredWidth(font) + 8;
    }

    @Override
    public int preferredHeight(Font font) {
        return content.preferredHeight(font) + 6;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        Font font = measureFont();
        int tipW = content.preferredWidth(font) + 8;
        int tipH = content.preferredHeight(font) + 6;

        int minX = lx + 2;
        int maxX = lx + Math.max(0, availableWidth - tipW - 2);
        int minY = ly + 2;
        int maxY = ly + Math.max(0, availableHeight - tipH - 2);

        int targetX = mouseX + 8;
        int targetY = mouseY + 12;

        if (targetX + tipW > lx + availableWidth) {
            targetX = Math.max(minX, mouseX - tipW - 4);
        }
        if (targetY + tipH > ly + availableHeight) {
            targetY = Math.max(minY, mouseY - tipH - 4);
        }

        targetX = Math.max(minX, Math.min(maxX, targetX));
        targetY = Math.max(minY, Math.min(maxY, targetY));

        setBounds(targetX, targetY, tipW, tipH);
        content.layoutTree(font, targetX + 4, targetY + 3, tipW - 8, tipH - 6);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.tooltipTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.tooltipTheme().radius(), colors.surfaceRaised(), colors.border());

        content.render(g, font, mx, my, pt);
    }

    private static final class TooltipText extends UIComponent {
        private final Component text;

        private TooltipText(Component text) {
            this.text = text;
        }

        @Override
        public int preferredWidth(Font font) {
            return font != null ? font.width(text) : 40;
        }

        @Override
        public int preferredHeight(Font font) {
            return font != null ? font.lineHeight : 9;
        }

        @Override
        public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
            g.drawString(font, text, x, y, theme().colors().onSurface(), false);
        }
    }
}
