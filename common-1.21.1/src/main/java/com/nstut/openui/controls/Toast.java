package com.nstut.openui.controls;

import com.nstut.openui.animation.Easing;
import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.overlay.OverlayManager;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;

public class Toast extends UIComponent {
    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private static final int MIN_WIDTH = 170;
    private static final int MAX_WIDTH = 280;
    private static final int MAX_VISIBLE_TOASTS = 4;

    private final Type type;
    private final Component title;
    private final Component message;
    private final long durationMillis;
    private final Runnable onDismiss;
    private OverlayHandle handle;
    private Component actionLabel;
    private Runnable actionCallback;
    private ButtonWidget actionButton;
    private ButtonWidget closeButton;
    private List<FormattedCharSequence> wrappedMessage;
    private long createdNanos = -1;
    private float progress = 1.0F;
    private boolean closed = false;

    public Toast(Type type, String title, String message) {
        this(type, Component.literal(title), Component.literal(message), 3500, null);
    }

    public Toast(Type type, Component title, Component message, long durationMillis, Runnable onDismiss) {
        this.type = type;
        this.title = Objects.requireNonNull(title);
        this.message = Objects.requireNonNull(message);
        this.durationMillis = durationMillis;
        this.onDismiss = onDismiss;

        this.closeButton = Ui.button("✕", this::dismiss).ghost().small();
        addChild(closeButton);
    }

    public Toast action(String label, Runnable callback) {
        return action(Component.literal(label), callback);
    }

    public Toast action(Component label, Runnable callback) {
        this.actionLabel = label;
        this.actionCallback = callback;
        if (actionButton != null) removeChild(actionButton);
        this.actionButton = Ui.button(label, () -> {
            dismiss();
            if (callback != null) callback.run();
        }).primary().small();
        addChild(this.actionButton);
        invalidateLayout();
        return this;
    }

    public void dismiss() {
        if (!closed) {
            closed = true;
            if (handle != null) handle.close();
            if (onDismiss != null) onDismiss.run();
        }
    }

    public static Toast info(String title, String message) { return new Toast(Type.INFO, title, message); }
    public static Toast success(String title, String message) { return new Toast(Type.SUCCESS, title, message); }
    public static Toast warning(String title, String message) { return new Toast(Type.WARNING, title, message); }
    public static Toast error(String title, String message) { return new Toast(Type.ERROR, title, message); }

    public static OverlayHandle show(OverlayManager overlays, Toast toast) {
        if (overlays == null || toast == null) return null;
        // Keep the newest toasts visible; dismiss the oldest beyond the cap so
        // a burst of notifications cannot stack off the bottom of the screen.
        int openToasts = 0;
        for (UIComponent c : overlays.components()) {
            if (c instanceof Toast t && !t.closed) openToasts++;
        }
        if (openToasts >= MAX_VISIBLE_TOASTS) {
            for (UIComponent c : overlays.components()) {
                if (c instanceof Toast t && !t.closed) {
                    t.dismiss();
                    break;
                }
            }
        }
        OverlayHandle handle = overlays.show(OverlayLayer.TOAST, toast, false, false, false, toast::dismiss);
        toast.handle = handle;
        return handle;
    }

    @Override
    public int preferredWidth(Font font) {
        if (font == null) return MIN_WIDTH;
        int tw = Math.max(font.width(title), font.width(message));
        int extra = (actionButton != null ? actionButton.preferredWidth(font) + 8 : 0) + 36;
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, tw + extra));
    }

    @Override
    public int preferredHeight(Font font) {
        // Single-line lower bound; layout() grows this when the message wraps.
        return font != null ? font.lineHeight * 2 + 18 : 34;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        Font f = measureFont();
        int w = preferredWidth(f);
        // Clamp to the viewport so long messages cannot push the toast off-screen.
        if (f != null && availableWidth > 0) {
            w = Math.min(w, Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, availableWidth - 20)));
        }
        int h;
        if (f != null) {
            wrappedMessage = f.split(message, Math.max(1, w - 28));
            h = f.lineHeight * (wrappedMessage.size() + 1) + 18;
        } else {
            wrappedMessage = List.of();
            h = preferredHeight(null);
        }

        // Stack multiple active toasts vertically
        int stackIndex = 0;
        if (runtime() != null && runtime().overlays() != null) {
            for (UIComponent c : runtime().overlays().components()) {
                if (c == this) break;
                if (c instanceof Toast) stackIndex++;
            }
        }

        int tx = lx + availableWidth - w - 10;
        int ty = ly + 10 + stackIndex * (h + 6);
        setBounds(tx, ty, w, h);

        closeButton.layout(tx + w - 16, ty + 4, 12, 12);
        if (actionButton != null) {
            int abw = actionButton.preferredWidth(f);
            actionButton.layout(tx + w - 20 - abw, ty + (h - 14) / 2, abw, 14);
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible || closed) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        if (createdNanos < 0) createdNanos = System.nanoTime();
        long elapsedNanos = System.nanoTime() - createdNanos;
        long durationNanos = durationMillis * 1_000_000L;

        if (elapsedNanos >= durationNanos) {
            dismiss();
            return;
        }

        progress = Math.max(0.0F, 1.0F - (float) elapsedNanos / durationNanos);

        // Entry slide-in transition
        long entryDurationNanos = 200_000_000L;
        float slideProgress = t.reducedMotion() ? 1.0F : Math.min(1.0F, (float) elapsedNanos / entryDurationNanos);
        float ease = Easing.EASE_OUT.apply(slideProgress);
        int slideOffset = Math.round((1.0F - ease) * 30);
        int drawX = x + slideOffset;

        int accentCol = switch (type) {
            case INFO -> colors.primary();
            case SUCCESS -> colors.success();
            case WARNING -> colors.warning();
            case ERROR -> colors.danger();
        };

        UiRender.shadow(g, drawX, y, width, height, t.toastTheme().radius());
        UiRender.roundedOutline(g, drawX, y, width, height, t.toastTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

        // Status indicator pill
        UiRender.roundedRect(g, drawX + 3, y + 4, 3, height - 8, 1, accentCol);

        // Title and wrapped Message
        UiRender.text(g, font, title, drawX + 12, y + 5, colors.onSurface());
        int messageY = y + 6 + font.lineHeight;
        List<FormattedCharSequence> lines = wrappedMessage != null ? wrappedMessage : List.of();
        if (lines.isEmpty()) {
            UiRender.text(g, font, message, drawX + 12, messageY, colors.onSurfaceMuted());
        } else {
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, drawX + 12, messageY, colors.onSurfaceMuted(), false);
                messageY += font.lineHeight;
            }
        }

        // Progress bar at bottom
        if (progress > 0.0F) {
            int barW = Math.round((width - 6) * progress);
            UiRender.roundedRect(g, drawX + 3, y + height - 3, barW, 2, 1, accentCol);
        }

        closeButton.layout(drawX + width - 16, y + 4, 12, 12);
        closeButton.render(g, font, mx, my, pt);

        if (actionButton != null) {
            int abw = actionButton.preferredWidth(font);
            actionButton.layout(drawX + width - 20 - abw, y + (height - 14) / 2, abw, 14);
            actionButton.render(g, font, mx, my, pt);
        }
    }
}
