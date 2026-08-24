package com.nstut.openui.controls;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Clips and vertically scrolls one child when its content exceeds the viewport. */
public class ScrollView extends UIComponent {
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int SCROLLBAR_GAP = 2;
    private static final int WHEEL_STEP = 24;
    private static final int DEFAULT_VIEWPORT = 160;

    private double scrollOffset;
    private int contentHeight;

    public ScrollView(UIComponent content) {
        addChild(content);
        content.fillWidth();
    }

    public UIComponent content() { return childCount() > 0 ? child(0) : null; }

    @Override public int preferredWidth(Font font) {
        UIComponent content = content();
        return content == null ? 0 : content.preferredWidth(font) + SCROLLBAR_WIDTH + SCROLLBAR_GAP;
    }

    @Override public int preferredHeight(Font font) {
        UIComponent content = content();
        return content == null ? 0 : Math.min(content.preferredHeight(font), DEFAULT_VIEWPORT);
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
    }

    private double maxScroll() { return Math.max(0, contentHeight - height); }

    private void clampScroll() {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        ClipStack.push(g, x, y, width, height);
        try { renderChildren(g, font, mx, my, pt); }
        finally { ClipStack.pop(g); }
        drawScrollbar(g);
    }

    private void drawScrollbar(GuiGraphics g) {
        if (maxScroll() <= 0 || width <= SCROLLBAR_WIDTH || height < SCROLLBAR_WIDTH * 4) return;
        var colors = theme().colors();
        int trackX = x + width - SCROLLBAR_WIDTH;
        int thumbHeight = Math.max(SCROLLBAR_WIDTH * 3, (int) ((long) height * height / contentHeight));
        float progress = (float) (scrollOffset / maxScroll());
        int thumbY = y + Math.round((height - thumbHeight) * progress);
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

    @Override public boolean mouseClicked(double mx, double my, int button) { return childrenMouseClicked(mx, my, button); }
    @Override public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) { return childrenMouseDragged(mx, my, button, dragX, dragY); }
    @Override public boolean mouseReleased(double mx, double my, int button) { return childrenMouseReleased(mx, my, button); }
}
