package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;
import java.util.WeakHashMap;

public class ClipStack extends Stack {
    // Only ownership depth is tracked here, per extractor. Rectangle transforms
    // and intersection belong to the native stack, which operates in screen space.
    private static final Map<GuiGraphicsExtractor, Integer> DEPTHS = new WeakHashMap<>();

    public static void push(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (graphics == null) return;
        graphics.enableScissor(x, y, x + width, y + height);
        DEPTHS.put(graphics, DEPTHS.getOrDefault(graphics, 0) + 1);
    }

    public static void pop(GuiGraphicsExtractor graphics) {
        if (graphics == null) return;
        Integer depth = DEPTHS.get(graphics);
        if (depth == null) return; // Never pop an incoming native/external clip.
        graphics.disableScissor();
        if (depth == 1) DEPTHS.remove(graphics);
        else DEPTHS.put(graphics, depth - 1);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        push(graphics, x, y, width, height);
        try {
            super.render(graphics, font, mouseX, mouseY, partialTick);
        } finally {
            pop(graphics);
        }
    }
}

