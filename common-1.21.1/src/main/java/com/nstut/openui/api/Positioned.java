package com.nstut.openui.api;

import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class Positioned extends UIComponent {
    private Integer left, top, right, bottom;

    public Positioned(UIComponent child) { addChild(child); }
    public Positioned left(int value) { left = value; invalidateLayout(); return this; }
    public Positioned top(int value) { top = value; invalidateLayout(); return this; }
    public Positioned right(int value) { right = value; invalidateLayout(); return this; }
    public Positioned bottom(int value) { bottom = value; invalidateLayout(); return this; }

    @Override public int preferredWidth(Font font) { return children.isEmpty() ? 0 : children.get(0).preferredWidth(font); }
    @Override public int preferredHeight(Font font) { return children.isEmpty() ? 0 : children.get(0).preferredHeight(font); }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        if (children.isEmpty()) return;
        UIComponent child = children.get(0);
        Size measured = child.measure(Constraints.loose(availableWidth, availableHeight), measureFont());
        int cw = left != null && right != null ? Math.max(0, availableWidth - left - right) : measured.width();
        int ch = top != null && bottom != null ? Math.max(0, availableHeight - top - bottom) : measured.height();
        int cx = left != null ? x + left : right != null ? x + availableWidth - right - cw : x;
        int cy = top != null ? y + top : bottom != null ? y + availableHeight - bottom - ch : y;
        child.layout(cx, cy, cw, ch);
    }

    @Override public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) { renderChildren(graphics, font, mouseX, mouseY, partialTick); }
}

