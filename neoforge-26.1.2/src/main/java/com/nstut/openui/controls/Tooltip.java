package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Tooltip extends UIComponent {
    private final UIComponent content;
    private int mouseX, mouseY;

    public Tooltip(String text) {
        this(Component.literal(text != null ? text : ""));
    }

    public Tooltip(Component text) {
        this(new TooltipText(splitLines(text)));
    }

    public Tooltip(UIComponent content) {
        this.content = Objects.requireNonNull(content);
        addChild(content);
    }

    public static List<Component> splitLines(Component text) {
        String raw = text != null ? text.getString() : "";
        List<Component> out = new ArrayList<>();
        for (String part : raw.split("\n", -1)) {
            out.add(Component.literal(part));
        }
        if (out.isEmpty()) out.add(Component.empty());
        return out;
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
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.tooltipTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.tooltipTheme().radius(), colors.surfaceRaised(), colors.border());

        content.render(g, font, mx, my, pt);
    }

    public static void drawHover(GuiGraphicsExtractor g, Font font, Component text,
                                 int mouseX, int mouseY,
                                 int boundsX, int boundsY, int boundsW, int boundsH) {
        drawHover(g, font, new TooltipText(splitLines(text)), mouseX, mouseY, boundsX, boundsY, boundsW, boundsH);
    }

    public static void drawHover(GuiGraphicsExtractor g, Font font, List<FormattedCharSequence> lines,
                                 int mouseX, int mouseY,
                                 int boundsX, int boundsY, int boundsW, int boundsH) {
        drawHover(g, font, new SequencesLines(lines), mouseX, mouseY, boundsX, boundsY, boundsW, boundsH);
    }

    private static void drawHover(GuiGraphicsExtractor g, Font font, UIComponent content,
                                  int mouseX, int mouseY,
                                  int boundsX, int boundsY, int boundsW, int boundsH) {
        Tooltip tip = new Tooltip(content);
        tip.setPosition(mouseX, mouseY);
        tip.layoutTree(font, boundsX, boundsY, boundsW, boundsH);
        tip.render(g, font, mouseX, mouseY, 1.0F);
    }

    private static final class TooltipText extends UIComponent {
        private final List<Component> lines;

        private TooltipText(List<Component> lines) {
            this.lines = lines;
        }

        @Override
        public int preferredWidth(Font font) {
            if (font == null) return 40;
            int w = 0;
            for (Component line : lines) {
                w = Math.max(w, font.width(line));
            }
            return w;
        }

        @Override
        public int preferredHeight(Font font) {
            int lh = font != null ? font.lineHeight : 9;
            return lines.size() * lh + Math.max(0, lines.size() - 1) * 2;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
            int color = theme().colors().onSurface();
            int lh = font != null ? font.lineHeight : 9;
            int ly = y;
            for (Component line : lines) {
                UiRender.text(g, font, line, x, ly, color);
                ly += lh + 2;
            }
        }
    }

    private static final class SequencesLines extends UIComponent {
        private final List<FormattedCharSequence> lines;

        private SequencesLines(List<FormattedCharSequence> lines) {
            this.lines = lines;
        }

        @Override
        public int preferredWidth(Font font) {
            if (font == null) return 40;
            int w = 0;
            for (FormattedCharSequence line : lines) {
                w = Math.max(w, font.width(line));
            }
            return w;
        }

        @Override
        public int preferredHeight(Font font) {
            int lh = font != null ? font.lineHeight : 9;
            return lines.size() * lh + Math.max(0, lines.size() - 1) * 2;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
            int color = theme().colors().onSurface();
            int lh = font != null ? font.lineHeight : 9;
            int ly = y;
            for (FormattedCharSequence line : lines) {
                g.text(font, line, x, ly, color, false);
                ly += lh + 2;
            }
        }
    }
}
