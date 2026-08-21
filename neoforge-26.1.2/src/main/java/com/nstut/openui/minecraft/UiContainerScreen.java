package com.nstut.openui.minecraft;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderBackgroundLayer(graphics, partialTick, mouseX, mouseY);
    }

    /**
     * Hook to render custom container backgrounds or slot frames before vanilla slot rendering.
     */
    protected void renderBackgroundLayer(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) { }

    /**
     * Hook to render foreground overlays above container items.
     */
    protected void renderForegroundLayer(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (uiRuntime != null) uiRuntime.render(graphics, mouseX, mouseY, partialTick);
        renderForegroundLayer(graphics, partialTick, mouseX, mouseY);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return uiRuntime != null && uiRuntime.mouseClicked(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick); }
    @Override public boolean mouseScrolled(double x, double y, double deltaX, double deltaY) { return uiRuntime != null && uiRuntime.mouseScrolled(x, y, deltaY) || super.mouseScrolled(x, y, deltaX, deltaY); }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) { return uiRuntime != null && uiRuntime.mouseDragged(event.x(), event.y(), event.button(), dx, dy) || super.mouseDragged(event, dx, dy); }
    @Override public boolean mouseReleased(MouseButtonEvent event) { return uiRuntime != null && uiRuntime.mouseReleased(event.x(), event.y(), event.button()) || super.mouseReleased(event); }
    @Override public boolean keyPressed(KeyEvent event) { return uiRuntime != null && uiRuntime.keyPressed(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event); }
    @Override public boolean keyReleased(KeyEvent event) { return uiRuntime != null && uiRuntime.keyReleased(event.key(), event.scancode(), event.modifiers()) || super.keyReleased(event); }
    @Override public boolean charTyped(CharacterEvent event) { return uiRuntime != null && uiRuntime.charTyped((char) event.codepoint(), 0) || super.charTyped(event); }

    @Override
    public void removed() {
        if (uiRuntime != null) uiRuntime.close();
        uiRuntime = null;
        super.removed();
    }
}
