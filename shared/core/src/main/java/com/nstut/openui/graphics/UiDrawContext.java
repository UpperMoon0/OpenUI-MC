package com.nstut.openui.graphics;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Stable renderer-neutral drawing SPI for reusable OpenUI components.
 * Version-specific raw graphics remain available through UiCanvas as an
 * explicit escape hatch, but component libraries should prefer this contract.
 */
public interface UiDrawContext {
    int width();
    int height();
    void fill(int x, int y, int width, int height, int color);
    void roundedRect(int x, int y, int width, int height, int radius, int color);
    void roundedOutline(int x, int y, int width, int height, int radius, int fillColor, int borderColor);

    /** Draws a rounded outline with an explicit border width using stable primitives only. */
    default void roundedOutline(
            int x,
            int y,
            int width,
            int height,
            int radius,
            int borderWidth,
            int fillColor,
            int borderColor) {
        if (width <= 0 || height <= 0) return;
        int thickness = Math.max(0, Math.min(borderWidth, Math.min(width, height) / 2));
        if (thickness == 0) {
            roundedRect(x, y, width, height, radius, fillColor);
            return;
        }
        roundedRect(x, y, width, height, radius, borderColor);
        int innerWidth = width - thickness * 2;
        int innerHeight = height - thickness * 2;
        if (innerWidth > 0 && innerHeight > 0) {
            roundedRect(x + thickness, y + thickness, innerWidth, innerHeight,
                    Math.max(0, radius - thickness), fillColor);
        }
    }

    void surface(int x, int y, int width, int height, int radius, int fillColor, int borderColor, boolean elevated);
    void shadow(int x, int y, int width, int height, int radius);
    void text(Component text, int x, int y, int color, boolean shadow);
    void text(String text, int x, int y, int color, boolean shadow);
    void texture(UiTexture texture, int x, int y, int u, int v, int width, int height);
    void renderItem(ItemStack stack, int x, int y);
    void tooltip(Component text, int mouseX, int mouseY, int boundsX, int boundsY, int boundsWidth, int boundsHeight);
    void pushClip(int x, int y, int width, int height);
    void popClip();
    void pushTransform();
    void translate(float dx, float dy);
    void scale(float sx, float sy);
    void popTransform();
}
