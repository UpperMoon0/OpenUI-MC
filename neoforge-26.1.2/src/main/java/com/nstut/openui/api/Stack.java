package com.nstut.openui.api;

import com.nstut.openui.layout.Alignment;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Stack extends UIComponent {
    private Alignment horizontal = Alignment.STRETCH;
    private Alignment vertical = Alignment.STRETCH;

    public Stack child(UIComponent child) { addChild(child); return this; }
    public Stack align(Alignment horizontal, Alignment vertical) { this.horizontal = horizontal; this.vertical = vertical; invalidateLayout(); return this; }

    @Override public int preferredWidth(Font font) { return children.stream().mapToInt(c -> c.preferredWidth(font)).max().orElse(0); }
    @Override public int preferredHeight(Font font) { return children.stream().mapToInt(c -> c.preferredHeight(font)).max().orElse(0); }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        for (UIComponent child : children) {
            Size size = child.measure(Constraints.loose(availableWidth, availableHeight), measureFont());
            int cw = horizontal == Alignment.STRETCH ? availableWidth : size.width();
            int ch = vertical == Alignment.STRETCH ? availableHeight : size.height();
            int cx = switch (horizontal) { case CENTER -> x + (availableWidth - cw) / 2; case END -> x + availableWidth - cw; default -> x; };
            int cy = switch (vertical) { case CENTER -> y + (availableHeight - ch) / 2; case END -> y + availableHeight - ch; default -> y; };
            child.layout(cx, cy, cw, ch);
        }
    }

    @Override public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) { renderChildren(graphics, font, mouseX, mouseY, partialTick); }
}

