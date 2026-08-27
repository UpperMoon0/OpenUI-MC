package com.nstut.openui.minecraft;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class UiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private UiRuntime uiRuntime;

    protected UiContainerScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract UIComponent buildUI();
    public final UiRuntime uiRuntime() { return uiRuntime; }

    @Override
    protected void init() {
        super.init();
        if (uiRuntime != null) uiRuntime.close();
        uiRuntime = new UiRuntime(font, new NativeWidgetHost() {
            @Override public void add(AbstractWidget widget) { UiContainerScreen.this.addWidget(widget); }
            @Override public void remove(AbstractWidget widget) { UiContainerScreen.this.removeWidget(widget); }
        });
        uiRuntime.setViewport(leftPos, topPos, imageWidth, imageHeight);
        uiRuntime.setRoot(buildUI());
    }

    protected final void rebuildUI() { if (uiRuntime != null) uiRuntime.setRoot(buildUI()); }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderBackgroundLayer(graphics, partialTick, mouseX, mouseY);
    }

    /**
     * Hook to render custom container backgrounds or slot frames before vanilla slot rendering.
     */
    protected void renderBackgroundLayer(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }

    /**
     * Hook to render foreground overlays above container items.
     */
    protected void renderForegroundLayer(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (uiRuntime != null) uiRuntime.render(graphics, mouseX, mouseY, partialTick);
        renderForegroundLayer(graphics, partialTick, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(double x, double y, int button) { return uiRuntime != null && uiRuntime.mouseClicked(x, y, button) || super.mouseClicked(x, y, button); }
    @Override public boolean mouseScrolled(double x, double y, double delta) { return uiRuntime != null && uiRuntime.mouseScrolled(x, y, delta) || super.mouseScrolled(x, y, delta); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return uiRuntime != null && uiRuntime.mouseDragged(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseReleased(double x, double y, int button) { return uiRuntime != null && uiRuntime.mouseReleased(x, y, button) || super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (uiRuntime != null && uiRuntime.keyPressed(key, scanCode, modifiers)) return true;
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scanCode)) return true;
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public boolean keyReleased(int key, int scanCode, int modifiers) { return uiRuntime != null && uiRuntime.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers); }
    @Override public boolean charTyped(char character, int modifiers) { return uiRuntime != null && uiRuntime.charTyped(character, modifiers) || super.charTyped(character, modifiers); }

    @Override
    public void removed() {
        if (uiRuntime != null) uiRuntime.close();
        uiRuntime = null;
        super.removed();
    }
}
