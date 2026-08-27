package com.nstut.openui.api;

import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.theme.TextStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class TextWidget extends UIComponent {

    private Supplier<Component> textSupplier;
    private TextStyle style = TextStyle.BODY;
    private Integer customColor;
    private boolean centered;
    private boolean wrap;
    private int maxLines = Integer.MAX_VALUE;
    private boolean ellipsis = true;
    private boolean marquee;

    public TextWidget(String text, int color, boolean centered) {
        this(Component.literal(text != null ? text : ""), color, centered);
    }

    public TextWidget(Component text, int color, boolean centered) {
        this(() -> text);
        this.customColor = color;
        this.centered = centered;
    }

    public TextWidget(String text) {
        this(Component.literal(text != null ? text : ""));
    }

    public TextWidget(Component text) {
        this(() -> text);
    }

    public TextWidget(Supplier<Component> textSupplier) {
        this.textSupplier = Objects.requireNonNull(textSupplier);
    }

    public void setText(String text) {
        setText(Component.literal(text != null ? text : ""));
    }

    public void setText(Component text) {
        this.textSupplier = () -> text;
        invalidateLayout();
    }

    public void setText(Supplier<Component> supplier) {
        this.textSupplier = Objects.requireNonNull(supplier);
        invalidateLayout();
    }

    public TextWidget style(TextStyle style) {
        this.style = Objects.requireNonNull(style);
        invalidateLayout();
        return this;
    }

    public TextWidget color(int color) {
        this.customColor = color;
        invalidatePaint();
        return this;
    }

    public TextWidget centered() {
        this.centered = true;
        invalidatePaint();
        return this;
    }

    public TextWidget centered(boolean centered) {
        this.centered = centered;
        invalidatePaint();
        return this;
    }

    public TextWidget wrap() {
        this.wrap = true;
        invalidateLayout();
        return this;
    }

    public TextWidget wrap(boolean wrap) {
        this.wrap = wrap;
        invalidateLayout();
        return this;
    }

    public TextWidget maxLines(int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        invalidateLayout();
        return this;
    }

    public TextWidget ellipsis() {
        return ellipsis(true);
    }

    public TextWidget ellipsis(boolean ellipsis) {
        this.ellipsis = ellipsis;
        invalidateLayout();
        return this;
    }

    public TextWidget marquee() {
        this.marquee = true;
        invalidatePaint();
        return this;
    }

    public static TextWidget label(String text, int color) { return new TextWidget(text, color, false); }
    public static TextWidget label(Component text, int color) { return new TextWidget(text, color, false); }
    public static TextWidget centered(String text, int color) { return new TextWidget(text, color, true); }
    public static TextWidget centered(Component text, int color) { return new TextWidget(text, color, true); }

    public Component getText() {
        return textSupplier != null ? textSupplier.get() : Component.empty();
    }

    @Override
    public int preferredWidth(Font font) {
        if (font == null) return 0;
        Component comp = getText();
        float scale = style != null ? style.scale() : 1.0F;
        int rawWidth = 0;
        try {
            rawWidth = font.width(comp);
        } catch (Throwable ignored) {
            rawWidth = comp.getString().length() * 6;
        }
        return Math.round(rawWidth * scale);
    }

    /**
     * Measures with the incoming width constraint applied, so wrapped text reports its
     * multi-line height on the first layout pass. {@link #preferredHeight(Font)} can only
     * use this widget's assigned {@code width}, which is still zero during measurement —
     * without this hint, a freshly built wrapped text is allocated a single line of
     * vertical space and overflows its parent once rendered.
     */
    @Override
    public Size measure(Constraints constraints, Font font) {
        Size size = super.measure(constraints, font);
        if (!wrap || font == null || hasRequestedHeight() || size.width() <= 0) {
            return size;
        }
        int previousWidth = width;
        this.width = size.width();
        try {
            return new Size(size.width(), super.measure(constraints, font).height());
        } finally {
            this.width = previousWidth;
        }
    }

    @Override
    public int preferredHeight(Font font) {
        if (font == null) return 10;
        float scale = style != null ? style.scale() : 1.0F;
        int baseLineHeight = 9;
        try {
            baseLineHeight = Math.round(font.lineHeight * scale);
        } catch (Throwable ignored) {}
        if (!wrap || width <= 0) {
            String str = getText().getString();
            int lineCount = Math.max(1, str.split("\n", -1).length);
            return lineCount * (baseLineHeight + 2);
        }
        try {
            int availableW = Math.max(10, Math.round(width / scale));
            List<FormattedCharSequence> lines = font.split(getText(), availableW);
            int lineCount = Math.min(maxLines, Math.max(1, lines.size()));
            return lineCount * (baseLineHeight + 2);
        } catch (Throwable ignored) {
            return baseLineHeight + 2;
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Component comp = getText();
        int textColor = customColor != null ? customColor
                : (style.colorOverride() != null ? style.colorOverride() : theme().colors().onSurface());

        float scale = style.scale();
        boolean useScale = Math.abs(scale - 1.0F) > 0.01F;

        if (useScale) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(scale, scale, 1.0F);
        }

        int drawX = useScale ? 0 : x;
        int drawY = useScale ? 0 : y;
        int effectiveWidth = useScale ? Math.round(width / scale) : width;

        if (wrap && effectiveWidth > 0) {
            List<FormattedCharSequence> lines = font.split(comp, effectiveWidth);
            int count = Math.min(maxLines, lines.size());
            for (int i = 0; i < count; i++) {
                FormattedCharSequence line = lines.get(i);
                if (ellipsis && i == maxLines - 1 && lines.size() > maxLines) {
                    // Ellipsize the final visible line
                    StringBuilder sb = new StringBuilder();
                    line.accept((index, styleVal, codePoint) -> {
                        sb.appendCodePoint(codePoint);
                        return true;
                    });
                    String lineStr = sb.toString();
                    int dotW = font.width("...");
                    String ell = font.plainSubstrByWidth(lineStr, Math.max(8, effectiveWidth - dotW)) + "...";
                    line = FormattedCharSequence.forward(ell, net.minecraft.network.chat.Style.EMPTY);
                }
                int lw = font.width(line);
                int lx = centered ? drawX + (effectiveWidth - lw) / 2 : drawX;
                int ly = drawY + i * (font.lineHeight + 2);
                g.drawString(font, line, lx, ly, textColor, style.shadow());
            }
        } else {
            String str = comp.getString();
            if (str.contains("\n")) {
                String[] lines = str.split("\n", -1);
                int count = Math.min(maxLines, lines.length);
                for (int i = 0; i < count; i++) {
                    String line = lines[i];
                    if (ellipsis && effectiveWidth > 0 && font.width(line) > effectiveWidth) {
                        line = font.plainSubstrByWidth(line, Math.max(8, effectiveWidth - font.width("..."))) + "...";
                    }
                    int lw = font.width(line);
                    int lx = centered ? drawX + (effectiveWidth - lw) / 2 : drawX;
                    int ly = drawY + i * (font.lineHeight + 2);
                    g.drawString(font, line, lx, ly, textColor, style.shadow());
                }
            } else {
                if (marquee && effectiveWidth > 0 && font.width(comp) > effectiveWidth) {
                    // Ping-pong the full string inside a hard clip instead of
                    // truncating it; UiAnimationUtil supplies the shared
                    // rest/slide/rest/return timing used across consumers.
                    int offset = UiAnimationUtil.pingPongOffset(font.width(comp), effectiveWidth, Util.getMillis());
                    ClipStack.push(g, x, y, width, height);
                    try {
                        g.drawString(font, comp, drawX - offset, drawY, textColor, style.shadow());
                    } finally {
                        ClipStack.pop(g);
                    }
                } else if (ellipsis && effectiveWidth > 0 && font.width(comp) > effectiveWidth) {
                    String trimmed = font.plainSubstrByWidth(str, Math.max(8, effectiveWidth - font.width("..."))) + "...";
                    int tw = font.width(trimmed);
                    int tx = centered ? drawX + (effectiveWidth - tw) / 2 : drawX;
                    g.drawString(font, trimmed, tx, drawY, textColor, style.shadow());
                } else {
                    int tw = font.width(comp);
                    int tx = centered ? drawX + (effectiveWidth - tw) / 2 : drawX;
                    g.drawString(font, comp, tx, drawY, textColor, style.shadow());
                }
            }
        }

        if (useScale) {
            g.pose().popPose();
        }
    }
}
