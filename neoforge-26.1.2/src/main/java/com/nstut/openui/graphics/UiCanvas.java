package com.nstut.openui.graphics;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.controls.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class UiCanvas implements UiDrawContext {
    private final GuiGraphicsExtractor graphics;
    private final Font font;

    public UiCanvas(GuiGraphicsExtractor graphics, Font font) {
        this.graphics = Objects.requireNonNull(graphics);
        this.font = font;
    }

    public GuiGraphicsExtractor rawGraphics() { return graphics; }
    public Font font() { return font; }
    public int width() { return graphics.guiWidth(); }
    public int height() { return graphics.guiHeight(); }

    public void fill(int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, color);
    }
    public void roundedRect(int x, int y, int width, int height, int radius, int color) { UiRender.roundedRect(graphics, x, y, width, height, radius, color); }
    public void roundedOutline(int x, int y, int width, int height, int radius, int fillColor, int borderColor) { UiRender.roundedOutline(graphics, x, y, width, height, radius, fillColor, borderColor); }
    public void surface(int x, int y, int width, int height, int radius, int fillColor, int borderColor, boolean elevated) { UiRender.surface(graphics, x, y, width, height, radius, fillColor, borderColor, elevated); }
    public void shadow(int x, int y, int width, int height, int radius) { UiRender.shadow(graphics, x, y, width, height, radius); }

    public void text(Component text, int x, int y, int color, boolean shadow) {
        if (font != null && text != null) graphics.text(font, text, x, y, color, shadow);
    }
    public void text(String text, int x, int y, int color, boolean shadow) {
        if (font != null && text != null) graphics.text(font, text, x, y, color, shadow);
    }

    public void texture(UiTexture texture, int x, int y, int u, int v, int width, int height) {
        Objects.requireNonNull(texture, "texture");
        Identifier location = Identifier.tryParse(texture.id());
        if (location == null) throw new IllegalArgumentException("Invalid texture id: " + texture.id());
        graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, u, v, width, height, 256, 256);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        if (stack != null && !stack.isEmpty()) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
    }

    public void tooltip(Component text, int mouseX, int mouseY, int boundsX, int boundsY, int boundsWidth, int boundsHeight) {
        if (font != null && text != null) {
            Tooltip.drawHover(graphics, font, text, mouseX, mouseY, boundsX, boundsY, boundsWidth, boundsHeight);
        }
    }

    public void pushClip(int x, int y, int width, int height) { ClipStack.push(graphics, x, y, width, height); }
    public void popClip() { ClipStack.pop(graphics); }
    public void pushTransform() { graphics.pose().pushMatrix(); }
    public void translate(float dx, float dy) { graphics.pose().translate(dx, dy); }
    public void scale(float sx, float sy) { graphics.pose().scale(sx, sy); }
    public void popTransform() { graphics.pose().popMatrix(); }
}
