package com.nstut.openui.graphics;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UiRender;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class UiCanvas {
    private final GuiGraphics graphics;
    private final Font font;

    public UiCanvas(GuiGraphics graphics, Font font) {
        this.graphics = Objects.requireNonNull(graphics);
        this.font = font;
    }

    public GuiGraphics rawGraphics() { return graphics; }
    public Font font() { return font; }

    public int width() { return graphics.guiWidth(); }
    public int height() { return graphics.guiHeight(); }

    public void fill(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, color);
    }

    public void roundedRect(int x, int y, int width, int height, int radius, int color) {
        UiRender.roundedRect(graphics, x, y, width, height, radius, color);
    }

    public void roundedOutline(int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        UiRender.roundedOutline(graphics, x, y, width, height, radius, fillColor, borderColor);
    }

    public void surface(int x, int y, int width, int height, int radius, int fillColor, int borderColor, boolean elevated) {
        UiRender.surface(graphics, x, y, width, height, radius, fillColor, borderColor, elevated);
    }

    public void shadow(int x, int y, int width, int height, int radius) {
        UiRender.shadow(graphics, x, y, width, height, radius);
    }

    public void text(Component text, int x, int y, int color, boolean shadow) {
        if (font == null || text == null) return;
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void text(String text, int x, int y, int color, boolean shadow) {
        if (font == null || text == null) return;
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public void pushClip(int x, int y, int width, int height) {
        ClipStack.push(graphics, x, y, width, height);
    }

    public void popClip() {
        ClipStack.pop(graphics);
    }

    public void pushTransform() {
        graphics.pose().pushPose();
    }

    public void translate(float dx, float dy) {
        graphics.pose().translate(dx, dy, 0);
    }

    public void scale(float sx, float sy) {
        graphics.pose().scale(sx, sy, 1.0F);
    }

    public void popTransform() {
        graphics.pose().popPose();
    }

    public void renderItem(ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }
    }
}
