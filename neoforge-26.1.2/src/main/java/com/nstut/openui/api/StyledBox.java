package com.nstut.openui.api;

import com.nstut.openui.graphics.UiCanvas;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

/** Reusable style wrapper without changing legacy component fluent APIs. */
public final class StyledBox extends UIComponent {
    private final UIComponent child;
    private StateStyle styles;
    private boolean pressed;
    private boolean disabled;

    public StyledBox(StateStyle styles, UIComponent child) {
        this.styles = Objects.requireNonNull(styles, "styles");
        this.child = Objects.requireNonNull(child, "child");
        addChild(child);
    }

    public StyledBox styles(StateStyle value) { styles = Objects.requireNonNull(value); invalidateLayout(); return this; }
    public StyledBox pressed(boolean value) { pressed = value; invalidatePaint(); return this; }
    public StyledBox disabled(boolean value) { disabled = value; invalidatePaint(); return this; }
    private Style resolved() { return styles.resolve(isHovered(), isFocused(), pressed, disabled); }

    @Override public int preferredWidth(Font font) {
        Style s = resolved();
        return child.preferredWidth(font) + s.margin().left() + s.margin().right() + s.padding().left() + s.padding().right();
    }
    @Override public int preferredHeight(Font font) {
        Style s = resolved();
        return child.preferredHeight(font) + s.margin().top() + s.margin().bottom() + s.padding().top() + s.padding().bottom();
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        Style s = resolved();
        int left = s.margin().left() + s.padding().left();
        int top = s.margin().top() + s.padding().top();
        int right = s.margin().right() + s.padding().right();
        int bottom = s.margin().bottom() + s.padding().bottom();
        child.layout(x + left, y + top, Math.max(0, availableWidth - left - right), Math.max(0, availableHeight - top - bottom));
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        Style s = resolved();
        int sx = x + s.margin().left();
        int sy = y + s.margin().top();
        int sw = Math.max(0, width - s.margin().left() - s.margin().right());
        int sh = Math.max(0, height - s.margin().top() - s.margin().bottom());
        UiCanvas canvas = new UiCanvas(g, font);
        int fill = s.background() == null ? 0 : s.background();
        if (s.borderColor() != null) canvas.roundedOutline(sx, sy, sw, sh, s.radius(), fill, s.borderColor());
        else if (s.background() != null) canvas.roundedRect(sx, sy, sw, sh, s.radius(), fill);
        child.render(g, font, mx, my, pt);
    }
}
