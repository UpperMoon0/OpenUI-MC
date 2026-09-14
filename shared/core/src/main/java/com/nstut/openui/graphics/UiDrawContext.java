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
    /** Additive glass-like surface with a portable gradient fallback; no framebuffer dependency. */
    default void surface(int x, int y, int width, int height, SurfaceStyle style) {
        SurfacePainter.paint(this::fill, x, y, width, height, style);
    }
    void fill(int x, int y, int width, int height, int color);

    /** Draws a rounded rectangle as disjoint fills so translucent pixels are composited exactly once. */
    default void roundedRect(int x, int y, int width, int height, int radius, int color) {
        RoundedGeometry.paintRounded(this::fill, x, y, width, height, radius, color);
    }

    /** Draws the traditional one-pixel rounded outline without painting border color under the fill. */
    default void roundedOutline(int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        roundedOutline(x, y, width, height, radius, 1, fillColor, borderColor);
    }

    /** Draws a rounded outline with an explicit border width using stable, disjoint primitives. */
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
        RoundedGeometry.paintRing(this::fill, x, y, width, height, radius, thickness, borderColor);
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

    /**
     * Compatibility convenience for the traditional 256x256 GUI texture case.
     * The requested destination size is also used as the source-region size.
     */
    default void texture(UiTexture texture, int x, int y, int u, int v, int width, int height) {
        texture(texture, x, y, u, v, width, height, width, height, 256, 256);
    }

    /**
     * Draws an arbitrary source region from a texture without assuming atlas dimensions.
     */
    void texture(
            UiTexture texture,
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            int srcWidth,
            int srcHeight,
            int textureWidth,
            int textureHeight);
    void renderItem(ItemStack stack, int x, int y);
    void tooltip(Component text, int mouseX, int mouseY, int boundsX, int boundsY, int boundsWidth, int boundsHeight);
    void pushClip(int x, int y, int width, int height);
    void popClip();
    void pushTransform();
    void translate(float dx, float dy);
    void scale(float sx, float sy);
    void popTransform();
}
