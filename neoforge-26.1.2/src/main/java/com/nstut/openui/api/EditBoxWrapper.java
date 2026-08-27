package com.nstut.openui.api;

import com.nstut.openui.runtime.NativeWidgetOwner;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

public class EditBoxWrapper extends UIComponent implements NativeWidgetOwner {

    private final EditBox editBox;
    private final int textColor;
    private final int bgColor;
    private String placeholder = "";
    private int radius = UiTheme.RADIUS_SM;

    public EditBoxWrapper(EditBox editBox) {
        this.editBox = java.util.Objects.requireNonNull(editBox);
        this.textColor = 0;
        this.bgColor = 0;
        this.editBox.setTextShadow(false);
        focusable(true);
    }

    public EditBoxWrapper(int maxLength, int textColor, int bgColor, Font font) {
        this.editBox = new EditBox(font, 0, 0, 100, 16, Component.empty());
        this.editBox.setMaxLength(maxLength);
        this.editBox.setBordered(false);
        this.editBox.setTextShadow(false);
        this.textColor = textColor;
        this.bgColor = bgColor;
        if (textColor != 0) this.editBox.setTextColor(textColor);
        focusable(true);
    }

    public EditBoxWrapper setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (this.editBox != null) {
            this.editBox.visible = visible;
            if (!visible) {
                this.editBox.setFocused(false);
            }
        }
    }

    public EditBox getEditBox() { return editBox; }
    @Override public EditBox nativeWidget() { return editBox; }
    public EditBoxWrapper radius(int radius) { this.radius = Math.max(0, radius); return this; }
    public String getValue() { return editBox.getValue(); }
    public void setValue(String v) {
        if (this.editBox != null) {
            String newVal = v != null ? v : "";
            this.editBox.setValue(newVal);
            this.editBox.setCursorPosition(newVal.length());
            this.editBox.setHighlightPos(newVal.length());
        }
    }
    @Override
    public boolean isFocused() {
        return super.isFocused() || (editBox != null && editBox.isFocused());
    }

    @Override
    public void onFocusGained() {
        super.onFocusGained();
        if (editBox != null) {
            editBox.setFocused(true);
        }
    }

    @Override
    public void onFocusLost() {
        super.onFocusLost();
        if (editBox != null) {
            editBox.setFocused(false);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        return editBox.keyPressed(new KeyEvent(key, scan, mod));
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return editBox.charTyped(new CharacterEvent(character));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = editBox.mouseClicked(
                new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
        if (handled) requestFocus();
        return handled;
    }

    @Override
    public int preferredWidth(Font font) { return 80; }
    @Override
    public int preferredHeight(Font font) { return 18; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        var colors = theme().colors();
        int border = editBox.isFocused() ? colors.primary() : (hovered ? colors.borderStrong() : colors.border());
        if (editBox.isFocused()) {
            UiRender.roundedRect(g, x - 1, y - 1, width + 2, height + 2, radius + 1,
                    UiRender.alpha(colors.primary(), 80));
        }
        int bg = bgColor != 0 ? bgColor : colors.input();
        UiRender.roundedOutline(g, x, y, width, height, radius, bg, border);

        // Keep editbox text color synchronized with active theme
        editBox.setTextColor(textColor != 0 ? textColor : colors.onSurface());
        editBox.setTextColorUneditable(colors.onSurfaceDisabled());

        // keep editbox inner bounds aligned with padding
        editBox.setX(x + 4);
        editBox.setY(y + (height - font.lineHeight) / 2);
        editBox.setWidth(Math.max(10, width - 8));

        if (editBox.getValue().isEmpty() && !placeholder.isEmpty()) {
            int placeholderColor = editBox.isFocused() ? colors.onSurface() : colors.onSurfaceMuted();
            int available = Math.max(1, width - 8);
            String visible = font.width(placeholder) <= available
                    ? placeholder
                    : font.plainSubstrByWidth(placeholder, Math.max(1, available - font.width("..."))) + "...";
            g.text(font, visible, x + 4, y + (height - font.lineHeight) / 2, placeholderColor, false);
        }
        // The native widget remains registered for input routing. Rendering it
        // here keeps its text and cursor above the component's styled surface.
        editBox.extractRenderState(g, mx, my, pt);
    }
}
