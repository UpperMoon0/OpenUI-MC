package com.nstut.openui.minecraft;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.graphics.LegacyGuiGraphics;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

public abstract class UiScreen extends Screen {
    private UiRuntime uiRuntime;

    protected UiScreen(ITextComponent title) { super(title); }

    protected abstract UIComponent buildUI();

    protected int uiLeft() { return 0; }
    protected int uiTop() { return 0; }
    protected int uiWidth() { return width; }
    protected int uiHeight() { return height; }

    public final UiRuntime uiRuntime() { return uiRuntime; }

    @Override
    protected void init() {
        if (uiRuntime != null) uiRuntime.close();
        uiRuntime = new UiRuntime(font, new NativeWidgetHost() {
            @Override public void add(Widget widget) { UiScreen.this.addWidget(widget); }
            @Override public void remove(Widget widget) { UiScreen.this.children.remove(widget); }
        });
        uiRuntime.setViewport(uiLeft(), uiTop(), uiWidth(), uiHeight());
        uiRuntime.setRoot(buildUI());
    }

    protected final void rebuildUI() {
        if (uiRuntime != null) uiRuntime.setRoot(buildUI());
    }

    @Override
    public void render(com.mojang.blaze3d.matrix.MatrixStack matrix, int mouseX, int mouseY, float partialTick) {
        super.render(matrix, mouseX, mouseY, partialTick);
        LegacyGuiGraphics graphics=new LegacyGuiGraphics(matrix);
        if (uiRuntime != null) uiRuntime.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (uiRuntime == null) return super.mouseClicked(x, y, button);
        if (uiRuntime.mouseClicked(x, y, button)) return true;
        return uiRuntime.nativeWidgets().withMouseClickFallbackSuppressed(() -> super.mouseClicked(x, y, button));
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) { return uiRuntime != null && uiRuntime.mouseScrolled(x, y, delta) || super.mouseScrolled(x, y, delta); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return uiRuntime != null && uiRuntime.mouseDragged(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseReleased(double x, double y, int button) { return uiRuntime != null && uiRuntime.mouseReleased(x, y, button) || super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) { return uiRuntime != null && uiRuntime.keyPressed(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers); }
    @Override public boolean keyReleased(int key, int scanCode, int modifiers) { return uiRuntime != null && uiRuntime.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers); }
    @Override public boolean charTyped(char character, int modifiers) { return uiRuntime != null && uiRuntime.charTyped(character, modifiers) || super.charTyped(character, modifiers); }

    @Override
    public void removed() {
        if (uiRuntime != null) uiRuntime.close();
        uiRuntime = null;
        super.removed();
    }
}
