package com.nstut.openui.api;

import com.nstut.openui.debug.LayoutDiagnostics;
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
    public StyledBox pressed(boolean value) {
        if (pressed != value) {
            pressed = value;
            invalidateLayout();
        }
        return this;
    }
    public StyledBox disabled(boolean value) {
        if (disabled != value) {
            disabled = value;
            invalidateLayout();
        }
        return this;
    }

    private Style resolved() { return styles.resolve(isHovered(), isFocusWithin(), pressed, disabled); }

    @Override
    protected void onHoverEnter() {
        invalidateLayout();
        super.onHoverEnter();
    }

    @Override
    protected void onHoverLeave() {
        invalidateLayout();
        super.onHoverLeave();
    }

    @Override
    public void onFocusGained() {
        invalidateLayout();
        super.onFocusGained();
    }

    @Override
    public void onFocusLost() {
        invalidateLayout();
        super.onFocusLost();
    }

    @Override
    public void onFocusWithinGained() {
        invalidateLayout();
        super.onFocusWithinGained();
    }

    @Override
    public void onFocusWithinLost() {
        invalidateLayout();
        super.onFocusWithinLost();
    }

    @Override public int preferredWidth(Font font) {
        Style s = resolved();
        int safe=surfaceSafeInset(s);
        int intrinsicSurface = child.preferredWidth(font) + Math.max(s.padding().left(),safe) + Math.max(s.padding().right(),safe);
        int surface = resolveAxis(intrinsicSurface, s.width(), s.minWidth(), s.maxWidth(), Integer.MAX_VALUE);
        return safeAdd(surface, s.margin().left() + s.margin().right());
    }

    @Override public int preferredHeight(Font font) {
        Style s = resolved();
        int safe=surfaceSafeInset(s);
        int intrinsicSurface = child.preferredHeight(font) + Math.max(s.padding().top(),safe) + Math.max(s.padding().bottom(),safe);
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

        int safe=surfaceSafeInset(s,surfaceWidth,surfaceHeight);
        int left=Math.max(s.padding().left(),safe), right=Math.max(s.padding().right(),safe);
        int top=Math.max(s.padding().top(),safe), bottom=Math.max(s.padding().bottom(),safe);
        int childX = x + s.margin().left() + left;
        int childY = y + s.margin().top() + top;
        int childWidth = Math.max(0, surfaceWidth - left - right);
        int childHeight = Math.max(0, surfaceHeight - top - bottom);
        LayoutDiagnostics.checkBoundedOverflow(this, child, childWidth, childHeight, measureFont());
        child.layout(childX, childY, childWidth, childHeight);
    }

    private static int clipInset(int padding,Style s,int surfaceWidth,int surfaceHeight) {
        return Math.max(padding,surfaceSafeInset(s,surfaceWidth,surfaceHeight));
    }
    private static int surfaceSafeInset(Style s) {
        int border=s.borderColor()!=null?Math.max(0,s.borderWidth()):0;
        int innerRadius=Math.max(0,s.radius()-border);
        return border+cornerSafeInset(innerRadius);
    }
    private static int surfaceSafeInset(Style s,int surfaceWidth,int surfaceHeight) {
        int maxThickness=Math.max(0,Math.min(surfaceWidth,surfaceHeight)/2);
        int border=s.borderColor()!=null?Math.min(Math.max(0,s.borderWidth()),maxThickness):0;
        int radius=Math.min(Math.max(0,s.radius()),maxThickness);
        int innerRadius=Math.max(0,radius-border);
        return border+cornerSafeInset(innerRadius);
    }
    private static int cornerSafeInset(int radius) {
        int r=Math.max(0,radius);
        if(r<=1) return 0;
        double rr=(double)r*r;
        return Math.max(0,(int)Math.ceil(r-(1.0D+Math.sqrt(8.0D*rr-1.0D))/4.0D));
    }
    @Override protected boolean clipsChildrenToBounds() { return true; }
    @Override protected int childClipX() {
        Style s=resolved(); int sw=surfaceWidth(s), sh=surfaceHeight(s);
        return x+s.margin().left()+clipInset(s.padding().left(),s,sw,sh);
    }
    @Override protected int childClipY() {
        Style s=resolved(); int sw=surfaceWidth(s), sh=surfaceHeight(s);
        return y+s.margin().top()+clipInset(s.padding().top(),s,sw,sh);
    }
    @Override protected int childClipWidth() {
        Style s=resolved(); int sw=surfaceWidth(s), sh=surfaceHeight(s);
        int left=clipInset(s.padding().left(),s,sw,sh), right=clipInset(s.padding().right(),s,sw,sh);
        return Math.max(0,sw-left-right);
    }
    @Override protected int childClipHeight() {
        Style s=resolved(); int sw=surfaceWidth(s), sh=surfaceHeight(s);
        int top=clipInset(s.padding().top(),s,sw,sh), bottom=clipInset(s.padding().bottom(),s,sw,sh);
        return Math.max(0,sh-top-bottom);
    }
    private int surfaceWidth(Style s) { return Math.max(0,width-s.margin().left()-s.margin().right()); }
    private int surfaceHeight(Style s) { return Math.max(0,height-s.margin().top()-s.margin().bottom()); }

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
        renderChildren(g, font, mx, my, pt);
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
