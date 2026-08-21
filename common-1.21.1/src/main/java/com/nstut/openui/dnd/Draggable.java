package com.nstut.openui.dnd;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.TextWidget;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.input.EventType;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.theme.Theme;
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
    private DragTarget<?> currentTarget;

    public Draggable(T data, UIComponent child) {
        this.data = Objects.requireNonNull(data);
        this.child = Objects.requireNonNull(child);
        addChild(child);
        on(EventType.MOUSE_DOWN, event -> event.capturePointer());
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
        // Always render the real child; apply a semi-transparent overlay while dragging
        child.render(g, font, mx, my, pt);
        if (dragging) {
            g.fill(x, y, x + width, y + height, 0x80000000);
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
                if (distSq >= 16) {
                    startDragging();
                }
            }
            if (dragging) {
                if (feedbackComponent != null) {
                    feedbackComponent.updatePosition(mx, my);
                }
                updateDragOverTarget(mx, my);
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
        clearDragOver();
        return false;
    }

    private void clearDragOver() {
        if (currentTarget != null) {
            currentTarget.setDragOver(false);
            currentTarget = null;
        }
    }

    private void updateDragOverTarget(double mx, double my) {
        if (runtime() == null || runtime().root() == null) return;
        UIComponent hit = runtime().root().hitTest((int) mx, (int) my);
        DragTarget<?> newTarget = findDragTarget(hit);
        if (newTarget != null && !newTarget.accepts(data)) newTarget = null;
        if (currentTarget != newTarget) {
            if (currentTarget != null) currentTarget.setDragOver(false);
            currentTarget = newTarget;
            if (currentTarget != null) currentTarget.setDragOver(true);
        }
    }

    private void startDragging() {
        dragging = true;
        currentTarget = null;
        invalidatePaint();
        if (runtime() != null) {
            // IMPORTANT: Never pass 'child' itself as feedback — that would reparent the live child
            // into the overlay and destroy the source component.
            // Use the caller-supplied factory if available; otherwise create a standalone GhostFeedback
            // that draws a copy using the source's current bounds.
            UIComponent fb;
            if (feedbackSupplier != null) {
                fb = feedbackSupplier.get();
            } else {
                fb = new GhostFeedback(child, child.getWidth(), child.getHeight());
            }
            feedbackComponent = new DragFeedbackOverlay(fb, (int) currentMouseX, (int) currentMouseY);
            dragOverlay = runtime().overlays().show(OverlayLayer.TOOLTIP, feedbackComponent, false, true, false, null);
        }
    }

    private void stopDragging(double mx, double my) {
        dragging = false;
        clearDragOver();
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
                @SuppressWarnings("unchecked")
                DragTarget<Object> casted = (DragTarget<Object>) target;
                if (casted.accepts(data)) {
                    casted.handleDrop(new DropEvent<>(data, this, mx, my));
                }
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

    // -------------------------------------------------------------------------
    // GhostFeedback: a lightweight stand-in that renders a styled preview
    // at the cursor position, avoiding reparenting or re-rendering the live
    // source child at its original coordinates.
    // -------------------------------------------------------------------------
    private static final class GhostFeedback extends UIComponent {
        private final UIComponent child;
        private final int srcW, srcH;

        private GhostFeedback(UIComponent child, int srcW, int srcH) {
            this.child = child;
            this.srcW = srcW;
            this.srcH = srcH;
        }

        @Override public int preferredWidth(Font font) { return srcW > 0 ? srcW : 40; }
        @Override public int preferredHeight(Font font) { return srcH > 0 ? srcH : 16; }

        @Override
        public void layout(int lx, int ly, int availableWidth, int availableHeight) {
            setBounds(lx, ly, availableWidth, availableHeight);
        }

        @Override
        public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
            if (!visible) return;
            Theme theme = theme();
            int cx = x;
            int cy = y;
            if (child instanceof TextWidget textWidget) {
                int textColor = theme.colors().onSurface();
                int fill = theme.colors().surface() | 0xDD000000;
                int border = theme.colors().border();
                UiRender.roundedOutline(g, cx, cy, srcW, srcH, 3, fill, border);
                int textH = font != null ? font.lineHeight : 9;
                g.drawString(font, textWidget.getText(), cx + 4, cy + (srcH - textH) / 2, textColor);
            } else {
                g.fill(cx, cy, cx + srcW, cy + srcH, theme.colors().surface() | 0xDD000000);
                UiRender.roundedOutline(g, cx, cy, srcW, srcH, 3, theme.colors().surface(), theme.colors().border());
            }
        }
    }

    // -------------------------------------------------------------------------
    // DragFeedbackOverlay: positions the feedback widget at the cursor.
    // -------------------------------------------------------------------------
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
