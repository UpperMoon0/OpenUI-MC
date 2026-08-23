package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
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

    /**
     * Records a new cursor anchor applied on the next overlay layout pass.
     * Deliberately does NOT invalidate the root layout; callers driving the
     * tooltip (the runtime hover tracker) request an overlay layout instead so
     * mouse movement never re-lays-out the whole component tree.
     */
    public void setPosition(int mx, int my) {
        this.mouseX = mx;
        this.mouseY = my;
        invalidatePaint();
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
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.tooltipTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.tooltipTheme().radius(), colors.surfaceRaised(), colors.border());

        content.render(g, font, mx, my, pt);
    }

    private static final class TooltipText extends UIComponent {
        /** Max text width before the runtime wraps onto further lines. */
        private static final int MAX_TEXT_WIDTH = 200;

        private final Component text;

        private TooltipText(Component text) {
            this.text = text;
        }

        private List<FormattedCharSequence> wrapped(Font font) {
            if (font == null) return List.of();
            return font.split(text, MAX_TEXT_WIDTH);
        }

        @Override
        public int preferredWidth(Font font) {
            if (font == null) return 40;
            return Math.min(font.width(text), MAX_TEXT_WIDTH);
        }

        @Override
        public int preferredHeight(Font font) {
            if (font == null) return 9;
            return Math.max(1, wrapped(font).size()) * (font.lineHeight + 1);
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
            if (font == null) return;
            int color = theme().colors().onSurface();
            int lineHeight = font.lineHeight + 1;
            int y = this.y;
            for (FormattedCharSequence line : wrapped(font)) {
                g.text(font, line, x, y, color);
                y += lineHeight;
            }
        }
    }
}
