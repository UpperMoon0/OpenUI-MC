package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Consumer;

public class IconWidget extends UIComponent {
    public enum Type { ITEM, TEXTURE, GLYPH, CUSTOM }

    private final Type type;
    private ItemStack itemStack;
    private Identifier texture;
    private int u, v, texWidth, texHeight;
    private String glyph;
    private Integer glyphColor;
    private int iconSize = 16;
    private Consumer<GuiGraphicsExtractor> customRenderer;

    private IconWidget(Type type) {
        this.type = type;
    }

    public static IconWidget of(ItemStack stack) {
        IconWidget w = new IconWidget(Type.ITEM);
        w.itemStack = Objects.requireNonNull(stack);
        w.iconSize = 16;
        return w;
    }

    public static IconWidget of(Identifier texture, int u, int v, int width, int height) {
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

    public static IconWidget custom(int size, Consumer<GuiGraphicsExtractor> renderer) {
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
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        int drawX = x + (width - iconSize) / 2;
        int drawY = y + (height - iconSize) / 2;

        switch (type) {
            case ITEM -> {
                if (itemStack != null && !itemStack.isEmpty()) {
                    g.item(itemStack, drawX, drawY);
                    g.itemDecorations(font, itemStack, drawX, drawY);
                }
            }
            case TEXTURE -> {
                if (texture != null) {
                    g.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY,
                            u, v, texWidth, texHeight, 256, 256);
                }
            }
            case GLYPH -> {
                if (glyph != null && font != null) {
                    int color = glyphColor != null ? glyphColor : theme().colors().primary();
                    int tw = font.width(glyph);
                    g.text(font, glyph, x + (width - tw) / 2, y + (height - font.lineHeight) / 2, color);
                }
            }
            case CUSTOM -> {
                if (customRenderer != null) {
                    g.pose().pushMatrix();
                    g.pose().translate(drawX, drawY);
                    customRenderer.accept(g);
                    g.pose().popMatrix();
                }
            }
        }
    }
}
