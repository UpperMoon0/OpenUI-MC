package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Clips a single child to its bounds and scrolls overflowing content with the
 * mouse wheel or a draggable scrollbar while the content exceeds the viewport.
 * Pair with {@code .flex()} inside a column so the viewport absorbs exactly the
 * leftover space instead of pushing siblings off screen.
 */
public class ScrollView extends UIComponent {
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_GAP = 2;
    private static final int WHEEL_STEP = 24;
    /** Natural viewport cap for un-flexed usage; flex parents override this. */
    private static final int DEFAULT_VIEWPORT = 160;

    private double scrollOffset;
    private int contentHeight;
    private boolean focusRevealPending;
    private UIComponent revealTarget;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;

    public ScrollView(UIComponent content) {
        addChild(content);
        content.fillWidth();
        on(com.nstut.openui.input.EventType.MOUSE_DOWN, event -> {
            if (event instanceof com.nstut.openui.input.PointerEvent pointer
                    && pointer.button() == 0 && scrollbarHit(pointer.x(), pointer.y())) event.capturePointer();
        });
    }

    /** Scroll position can be retained across consumer screen resizing/rebuilds. */
    public double scrollOffset() { return scrollOffset; }
    public ScrollView scrollTo(double offset) {
        if(!Double.isFinite(offset)) throw new IllegalArgumentException("Invalid scroll offset");
        scrollOffset=Math.max(0,offset);invalidateLayout();return this;
    }
    /** Reveal a changed/expanded descendant after the next layout, without stealing keyboard focus. */
    public ScrollView reveal(UIComponent target) {
        if(target==null || !isDescendant(target)) throw new IllegalArgumentException("Target must belong to this scroll view");
        revealTarget=target; invalidateLayout(); return this;
    }
    public UIComponent content() { return childCount() > 0 ? child(0) : null; }

    @Override public int preferredWidth(Font font) {
        UIComponent content = content();
        return content == null ? 0 : content.preferredWidth(font) + SCROLLBAR_WIDTH + SCROLLBAR_GAP;
    }

    @Override public int preferredHeight(Font font) {
        UIComponent content = content();
        if (content == null) return 0;
        return Math.min(content.preferredHeight(font), DEFAULT_VIEWPORT);
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        UIComponent content = content();
        if (content == null) {
            contentHeight = 0;
            clampScroll();
            return;
        }
        int contentWidth = Math.max(0, availableWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
        Size measured = content.measure(new Constraints(contentWidth, contentWidth, 0, Constraints.INFINITY), measureFont());
        contentHeight = Math.max(measured.height(), availableHeight);
        clampScroll();
        content.layout(x, y - (int) scrollOffset, contentWidth, contentHeight);
        if (focusRevealPending || revealTarget!=null) {
            focusRevealPending=false;
            UIComponent focused=revealTarget!=null?revealTarget:runtime()!=null?runtime().focus().focused():null;
            revealTarget=null;
            if (focused!=null&&isDescendant(focused)) {
                double next=scrollOffsetForReveal(focused);
                if (Double.compare(next,scrollOffset)!=0) {
                    scrollOffset=next;
                    content.layout(x,y-(int)scrollOffset,contentWidth,contentHeight);
                }
            }
        }
    }

    private double maxScroll() { return Math.max(0, contentHeight - height); }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
        if (maxScroll() <= 0) draggingScrollbar = false;
    }

    @Override protected boolean clipsChildrenToBounds() { return true; }

    @Override protected boolean isFocusNavigationViewport() { return maxScroll()>0; }

    @Override protected void revealFocusedDescendant(UIComponent descendant) {
        if (descendant==null||!isDescendant(descendant)) return;
        focusRevealPending=true;
        invalidateLayout();
    }

    private boolean isDescendant(UIComponent component) {
        for (UIComponent cursor=component; cursor!=null; cursor=cursor.parent()) {
            if (cursor==this) return true;
        }
        return false;
    }

    private double scrollOffsetForReveal(UIComponent descendant) {
        if (height<=0||maxScroll()<=0||descendant.getHeight()<=0) return scrollOffset;
        double next=scrollOffset;
        int targetTop=descendant.getY();
        int targetBottom=targetTop+descendant.getHeight();
        int viewportTop=y;
        int viewportBottom=y+height;
        if (targetTop<viewportTop) next-=viewportTop-targetTop;
        else if (targetBottom>viewportBottom) next+=targetBottom-viewportBottom;
        return Math.max(0,Math.min(maxScroll(),next));
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        renderChildren(g, font, mx, my, pt);
        drawScrollbar(g);
    }

    private void drawScrollbar(GuiGraphics g) {
        if (!hasScrollbar()) return;
        var colors = theme().colors();
        int trackX = x + width - SCROLLBAR_WIDTH;
        int thumbHeight = scrollbarThumbHeight();
        int thumbY = scrollbarThumbY();
        UiRender.pill(g, trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight,
                UiRender.mix(colors.borderSubtle(), colors.onSurfaceMuted(), 0.35F), 0);
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < x || mx >= x + width || my < y || my >= y + height || delta == 0) return false;
        if (childrenMouseScrolled(mx, my, delta)) return true;
        double before = scrollOffset;
        scrollOffset -= delta * WHEEL_STEP;
        clampScroll();
        if (Double.compare(before, scrollOffset) == 0) return false;
        invalidateLayout();
        return true;
    }

    private boolean hasScrollbar() {
        return maxScroll() > 0 && width > SCROLLBAR_WIDTH && height >= SCROLLBAR_WIDTH * 4;
    }
    private int scrollbarThumbHeight() {
        return Math.min(height, Math.max(SCROLLBAR_WIDTH * 3, (int) ((long) height * height / contentHeight)));
    }
    private int scrollbarThumbY() {
        return y + (int) Math.round((height - scrollbarThumbHeight()) * scrollOffset / maxScroll());
    }
    private boolean scrollbarHit(double mx, double my) {
        return visible && hasScrollbar() && mx >= x + width - SCROLLBAR_WIDTH - SCROLLBAR_GAP
                && mx < x + width && my >= y && my < y + height;
    }
    @Override public UIComponent hitTest(int mx, int my) {
        // The scrollbar owns its gutter even when a child paints beyond its measured width.
        return scrollbarHit(mx,my) ? this : super.hitTest(mx,my);
    }
    private void dragScrollbar(double mouseY) {
        int travel = height - scrollbarThumbHeight();
        if (travel <= 0) return;
        scrollOffset = Math.max(0, Math.min(1, (mouseY - y - scrollbarGrabOffset) / travel)) * maxScroll();
        invalidateLayout();
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        draggingScrollbar = false;
        if (button == 0 && scrollbarHit(mx,my)) {
            int thumbY = scrollbarThumbY(), thumbHeight = scrollbarThumbHeight();
            boolean onThumb = my >= thumbY && my < thumbY + thumbHeight;
            // Rendering rounds the thumb to a pixel; capture its logical position so
            // pressing or moving a long-document thumb cannot quantize the offset.
            double logicalThumbY = y + (height - thumbHeight) * scrollOffset / maxScroll();
            scrollbarGrabOffset = onThumb ? my - logicalThumbY : thumbHeight / 2.0;
            draggingScrollbar = true;
            if (!onThumb) dragScrollbar(my);
            return true;
        }
        return childrenMouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) { dragScrollbar(my); return true; }
        return childrenMouseDragged(mx,my,button,dragX,dragY);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) { draggingScrollbar = false; return true; }
        return childrenMouseReleased(mx,my,button);
    }
    @Override public void pointerCancelled() { draggingScrollbar = false; }
    @Override protected void onUnmount() { draggingScrollbar = false; }
}
