package com.nstut.openui.controls;

import com.nstut.openui.api.UiRender;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;

public class IconWidget extends UIComponent {
    public enum Type { ITEM, TEXTURE, GLYPH, CUSTOM }

    private final Type type;
    private ItemStack itemStack;
    private ResourceLocation texture;
    private int u, v, texWidth, texHeight;
    private String glyph;
    private Integer glyphColor;
    private int iconSize = 16;
    private Consumer<GuiGraphics> customRenderer;

    private IconWidget(Type type) {
        this.type = type;
    }

    public static IconWidget of(ItemStack stack) {
        IconWidget w = new IconWidget(Type.ITEM);
        w.itemStack = Objects.requireNonNull(stack);
        w.iconSize = 16;
        return w;
    }

    public static IconWidget of(ResourceLocation texture, int u, int v, int width, int height) {
        IconWidget w = new IconWidget(Type.TEXTURE);
        w.texture = Objects.requireNonNull(texture);
        w.u = u;
        w.v = v;
        w.texWidth = width;
        w.texHeight = height;
        w.iconSize = Math.max(width, height);
        return w;
    }

    public static IconWidget of(String glyph, int color) {
        IconWidget w = new IconWidget(Type.GLYPH);
        w.glyph = Objects.requireNonNull(glyph);
        w.glyphColor = color;
        w.iconSize = 12;
        return w;
    }

    public static IconWidget custom(int size, Consumer<GuiGraphics> renderer) {
        IconWidget w = new IconWidget(Type.CUSTOM);
        w.iconSize = size;
        w.customRenderer = Objects.requireNonNull(renderer);
        return w;
    }

    public IconWidget size(int size) {
        this.iconSize = Math.max(4, size);
        invalidateLayout();
        return this;
    }

    @Override
    public int preferredWidth(Font font) {
        return iconSize;
    }

    @Override
    public int preferredHeight(Font font) {
        return iconSize;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        int drawX = x + (width - iconSize) / 2;
        int drawY = y + (height - iconSize) / 2;

        switch (type) {
            case ITEM -> {
                if (itemStack != null && !itemStack.isEmpty()) {
                    g.renderItem(itemStack, drawX, drawY);
                    g.renderItemDecorations(font, itemStack, drawX, drawY);
                }
            }
            case TEXTURE -> {
                if (texture != null) {
                    g.blit(texture, drawX, drawY, u, v, texWidth, texHeight);
                }
            }
            case GLYPH -> {
                if (glyph != null && font != null) {
                    int color = glyphColor != null ? glyphColor : theme().colors().primary();
                    int tw = font.width(glyph);
                    UiRender.text(g, font, glyph, x + (width - tw) / 2, y + (height - font.lineHeight) / 2, color);
                }
            }
            case CUSTOM -> {
                if (customRenderer != null) {
                    g.pose().pushPose();
                    g.pose().translate(drawX, drawY, 0);
                    customRenderer.accept(g);
                    g.pose().popPose();
                }
            }
        }
    }
}
