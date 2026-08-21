package com.nstut.openui.minecraft;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class UiScreen extends Screen {
    private UiRuntime uiRuntime;

    protected UiScreen(Component title) { super(title); }

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
            @Override public void add(AbstractWidget widget) { UiScreen.this.addWidget(widget); }
            @Override public void remove(AbstractWidget widget) { UiScreen.this.removeWidget(widget); }
        });
        uiRuntime.setViewport(uiLeft(), uiTop(), uiWidth(), uiHeight());
        uiRuntime.setRoot(buildUI());
    }

    protected final void rebuildUI() {
        if (uiRuntime != null) uiRuntime.setRoot(buildUI());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (uiRuntime != null) uiRuntime.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(double x, double y, int button) { return uiRuntime != null && uiRuntime.mouseClicked(x, y, button) || super.mouseClicked(x, y, button); }
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

