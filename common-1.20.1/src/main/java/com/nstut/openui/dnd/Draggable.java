package com.nstut.openui.dnd;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;
import java.util.function.Supplier;

public class Draggable<T> extends UIComponent {
    private final T data;
    private final UIComponent child;
    private Supplier<UIComponent> feedbackSupplier;
    private boolean dragging;
    private double startMouseX, startMouseY;
    private double currentMouseX, currentMouseY;
    private OverlayHandle dragOverlay;
    private DragFeedbackOverlay feedbackComponent;

    public Draggable(T data, UIComponent child) {
        this.data = Objects.requireNonNull(data);
        this.child = Objects.requireNonNull(child);
        addChild(child);
    }

    public Draggable<T> feedback(Supplier<UIComponent> feedbackSupplier) {
        this.feedbackSupplier = feedbackSupplier;
        return this;
    }

    public T getData() { return data; }
    public boolean isDragging() { return dragging; }

    @Override
    public int preferredWidth(Font font) {
        return child.preferredWidth(font);
    }

    @Override
    public int preferredHeight(Font font) {
        return child.preferredHeight(font);
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        child.layoutTree(measureFont(), lx, ly, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (dragging) {
            // Render dimmed/translucent
            child.render(g, font, mx, my, pt);
            g.fill(x, y, x + width, y + height, 0x80000000);
        } else {
            child.render(g, font, mx, my, pt);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= x && mx < x + width && my >= y && my < y + height) {
            startMouseX = mx;
            startMouseY = my;
            currentMouseX = mx;
            currentMouseY = my;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn == 0) {
            currentMouseX = mx;
            currentMouseY = my;
            if (!dragging) {
                double distSq = (mx - startMouseX) * (mx - startMouseX) + (my - startMouseY) * (my - startMouseY);
                if (distSq >= 16) { // 4 pixel drag threshold
                    startDragging();
                }
            }
            if (dragging && feedbackComponent != null) {
                feedbackComponent.updatePosition(mx, my);
            }
            return dragging;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0 && dragging) {
            stopDragging(mx, my);
            return true;
        }
        dragging = false;
        return false;
    }

    private void startDragging() {
        dragging = true;
        invalidatePaint();
        if (runtime() != null) {
            UIComponent fb = (feedbackSupplier != null) ? feedbackSupplier.get() : child;
            feedbackComponent = new DragFeedbackOverlay(fb, (int) currentMouseX, (int) currentMouseY);
            dragOverlay = runtime().overlays().show(OverlayLayer.TOOLTIP, feedbackComponent, false, true, false, null);
        }
    }

    private void stopDragging(double mx, double my) {
        dragging = false;
        if (dragOverlay != null) {
            dragOverlay.close();
            dragOverlay = null;
            feedbackComponent = null;
        }
        invalidatePaint();

        if (runtime() != null && runtime().root() != null) {
            UIComponent hit = runtime().root().hitTest((int) mx, (int) my);
            DragTarget<?> target = findDragTarget(hit);
            if (target != null) {
                target.handleDrop(new DropEvent<>(data, this, mx, my));
            }
        }
    }

    private DragTarget<?> findDragTarget(UIComponent comp) {
        UIComponent cursor = comp;
        while (cursor != null) {
            if (cursor instanceof DragTarget<?> dt) return dt;
            cursor = cursor.parent();
        }
        return null;
    }

    private static final class DragFeedbackOverlay extends UIComponent {
        private final UIComponent feedback;
        private int fx, fy;

        private DragFeedbackOverlay(UIComponent feedback, int fx, int fy) {
            this.feedback = feedback;
            this.fx = fx;
            this.fy = fy;
            addChild(feedback);
        }

        private void updatePosition(double mx, double my) {
            this.fx = (int) mx;
            this.fy = (int) my;
            invalidatePaint();
        }

        @Override public int preferredWidth(Font font) { return feedback.preferredWidth(font); }
        @Override public int preferredHeight(Font font) { return feedback.preferredHeight(font); }

        @Override
        public void layout(int lx, int ly, int availableWidth, int availableHeight) {
            setBounds(0, 0, availableWidth, availableHeight);
            Font f = measureFont();
            int fw = feedback.preferredWidth(f);
            int fh = feedback.preferredHeight(f);
            feedback.layoutTree(f, fx - fw / 2, fy - fh / 2, fw, fh);
        }

        @Override
        public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
            feedback.render(g, font, mx, my, pt);
        }
    }
}
