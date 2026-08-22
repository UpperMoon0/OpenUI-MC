package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Deque;

public class ClipStack extends Stack {
    private static final Deque<ScissorRect> STACK = new ArrayDeque<>();

    private record ScissorRect(int minX, int minY, int maxX, int maxY) {
        ScissorRect intersect(int x, int y, int w, int h) {
            int newMinX = Math.max(minX, x);
            int newMinY = Math.max(minY, y);
            int newMaxX = Math.max(newMinX, Math.min(maxX, x + w));
            int newMaxY = Math.max(newMinY, Math.min(maxY, y + h));
            return new ScissorRect(newMinX, newMinY, newMaxX, newMaxY);
        }
    }

    public static void push(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics == null) return;
        ScissorRect current = STACK.peek();
        ScissorRect next = current != null ? current.intersect(x, y, width, height) : new ScissorRect(x, y, x + width, y + height);
        STACK.push(next);
        graphics.enableScissor(next.minX(), next.minY(), next.maxX(), next.maxY());
    }

    public static void pop(GuiGraphics graphics) {
        if (graphics == null || STACK.isEmpty()) return;
        STACK.pop();
        // GuiGraphics maintains its own nested scissor stack. Popping once
        // automatically restores the previous rectangle; enabling it again
        // would push another scissor and leak render state across frames.
        graphics.disableScissor();
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        push(graphics, x, y, width, height);
        try {
            super.render(graphics, font, mouseX, mouseY, partialTick);
        } finally {
            pop(graphics);
        }
    }
}

