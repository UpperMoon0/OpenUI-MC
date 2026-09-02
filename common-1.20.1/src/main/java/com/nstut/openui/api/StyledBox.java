package com.nstut.openui.api;

import com.nstut.openui.graphics.UiCanvas;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

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
        int intrinsicSurface = child.preferredWidth(font) + s.padding().left() + s.padding().right();
        int surface = resolveAxis(intrinsicSurface, s.width(), s.minWidth(), s.maxWidth(), Integer.MAX_VALUE);
        return safeAdd(surface, s.margin().left() + s.margin().right());
    }

    @Override public int preferredHeight(Font font) {
        Style s = resolved();
        int intrinsicSurface = child.preferredHeight(font) + s.padding().top() + s.padding().bottom();
        int surface = resolveAxis(intrinsicSurface, s.height(), s.minHeight(), s.maxHeight(), Integer.MAX_VALUE);
        return safeAdd(surface, s.margin().top() + s.margin().bottom());
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        Style s = resolved();
        int horizontalMargin = s.margin().left() + s.margin().right();
        int verticalMargin = s.margin().top() + s.margin().bottom();
        int maxSurfaceWidth = Math.max(0, availableWidth - horizontalMargin);
        int maxSurfaceHeight = Math.max(0, availableHeight - verticalMargin);
        int surfaceWidth = resolveAxis(maxSurfaceWidth, s.width(), s.minWidth(), s.maxWidth(), maxSurfaceWidth);
        int surfaceHeight = resolveAxis(maxSurfaceHeight, s.height(), s.minHeight(), s.maxHeight(), maxSurfaceHeight);
        int outerWidth = Math.min(availableWidth, safeAdd(surfaceWidth, horizontalMargin));
        int outerHeight = Math.min(availableHeight, safeAdd(surfaceHeight, verticalMargin));
        setBounds(x, y, outerWidth, outerHeight);

        int childX = x + s.margin().left() + s.padding().left();
        int childY = y + s.margin().top() + s.padding().top();
        int childWidth = Math.max(0, surfaceWidth - s.padding().left() - s.padding().right());
        int childHeight = Math.max(0, surfaceHeight - s.padding().top() - s.padding().bottom());
        child.layout(childX, childY, childWidth, childHeight);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        Style s = resolved();
        int sx = x + s.margin().left();
        int sy = y + s.margin().top();
        int sw = Math.max(0, width - s.margin().left() - s.margin().right());
        int sh = Math.max(0, height - s.margin().top() - s.margin().bottom());
        UiCanvas canvas = new UiCanvas(g, font);
        int fill = s.background() == null ? 0 : s.background();
        if (s.borderColor() != null && s.borderWidth() > 0) {
            canvas.roundedOutline(sx, sy, sw, sh, s.radius(), s.borderWidth(), fill, s.borderColor());
        } else if (s.background() != null) {
            canvas.roundedRect(sx, sy, sw, sh, s.radius(), fill);
        }
        child.render(g, font, mx, my, pt);
    }

    private static int resolveAxis(int intrinsic, Integer exact, Integer min, Integer max, int available) {
        long value = exact != null ? exact : intrinsic;
        if (min != null) value = Math.max(value, min);
        if (max != null) value = Math.min(value, max);
        value = Math.max(0L, value);
        if (available != Integer.MAX_VALUE) value = Math.min(value, Math.max(0, available));
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static int safeAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
