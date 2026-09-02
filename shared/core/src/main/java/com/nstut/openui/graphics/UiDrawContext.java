package com.nstut.openui.graphics;

import net.minecraft.network.chat.Component;

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
    void surface(int x, int y, int width, int height, int radius, int fillColor, int borderColor, boolean elevated);
    void shadow(int x, int y, int width, int height, int radius);
    void text(Component text, int x, int y, int color, boolean shadow);
    void text(String text, int x, int y, int color, boolean shadow);
    void pushClip(int x, int y, int width, int height);
    void popClip();
}
