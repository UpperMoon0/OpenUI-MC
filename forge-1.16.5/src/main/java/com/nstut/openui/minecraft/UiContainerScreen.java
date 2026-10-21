package com.nstut.openui.minecraft;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.graphics.LegacyGuiGraphics;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;

public abstract class UiContainerScreen<T extends Container> extends ContainerScreen<T> {
    private UiRuntime uiRuntime;

    protected UiContainerScreen(T menu, PlayerInventory inventory, ITextComponent title) {
        super(menu, inventory, title);
    }

    protected abstract UIComponent buildUI();
    public final UiRuntime uiRuntime() { return uiRuntime; }

    @Override
    protected void init() {
        super.init();
        if (uiRuntime != null) uiRuntime.close();
        uiRuntime = new UiRuntime(font, new NativeWidgetHost() {
            @Override public void add(Widget widget) { UiContainerScreen.this.addWidget(widget); }
            @Override public void remove(Widget widget) { UiContainerScreen.this.children.remove(widget); }
        });
        uiRuntime.setViewport(leftPos, topPos, imageWidth, imageHeight);
        uiRuntime.setRoot(buildUI());
    }

    protected final void rebuildUI() { if (uiRuntime != null) uiRuntime.setRoot(buildUI()); }

    @Override
    protected void renderBg(com.mojang.blaze3d.matrix.MatrixStack matrix, float partialTick, int mouseX, int mouseY) {
        renderBackgroundLayer(new LegacyGuiGraphics(matrix), partialTick, mouseX, mouseY);
    }

    /**
     * Hook to render custom container backgrounds or slot frames before vanilla slot rendering.
     */
    protected void renderBackgroundLayer(LegacyGuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }

    /**
     * Hook to render foreground overlays above container items.
     */
    protected void renderForegroundLayer(LegacyGuiGraphics graphics, float partialTick, int mouseX, int mouseY) { }

    @Override
    public void render(com.mojang.blaze3d.matrix.MatrixStack matrix, int mouseX, int mouseY, float partialTick) {
        super.render(matrix, mouseX, mouseY, partialTick);
        LegacyGuiGraphics graphics=new LegacyGuiGraphics(matrix);
        if (uiRuntime != null) uiRuntime.render(graphics, mouseX, mouseY, partialTick);
        renderForegroundLayer(graphics, partialTick, mouseX, mouseY);
        if (uiRuntime == null || !uiRuntime.overlays().suppressesUnderlyingTooltips(mouseX, mouseY)) {
            renderTooltip(matrix, mouseX, mouseY);
        }
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (uiRuntime == null) return super.mouseClicked(x, y, button);
        if (uiRuntime.mouseClicked(x, y, button)) return true;
        return uiRuntime.nativeWidgets().withMouseClickFallbackSuppressed(() -> super.mouseClicked(x, y, button));
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) { return uiRuntime != null && uiRuntime.mouseScrolled(x, y, delta) || super.mouseScrolled(x, y, delta); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) { return uiRuntime != null && uiRuntime.mouseDragged(x, y, button, dx, dy) || super.mouseDragged(x, y, button, dx, dy); }
    @Override public boolean mouseReleased(double x, double y, int button) { return uiRuntime != null && uiRuntime.mouseReleased(x, y, button) || super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        boolean inventoryKey = minecraft != null && minecraft.options.keyInventory.matches(key, scanCode);
        return ContainerKeyPolicy.dispatch(key, inventoryKey,
                uiRuntime != null && uiRuntime.hasTextInputFocus(),
                uiRuntime != null && uiRuntime.overlays().hasBlockingOverlay(),
                () -> uiRuntime != null && uiRuntime.keyPressed(key, scanCode, modifiers),
                () -> super.keyPressed(key, scanCode, modifiers));
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
