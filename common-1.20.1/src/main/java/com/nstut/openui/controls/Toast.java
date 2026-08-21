package com.nstut.openui.controls;

import com.nstut.openui.animation.Easing;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.overlay.OverlayManager;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class Toast extends UIComponent {
    public enum Type { INFO, SUCCESS, WARNING, ERROR }

    private final Type type;
    private final Component title;
    private final Component message;
    private final long durationMillis;
    private final Runnable onDismiss;
    private long createdNanos = -1;
    private float progress = 1.0F;

    public Toast(Type type, String title, String message) {
        this(type, Component.literal(title), Component.literal(message), 3000, null);
    }

    public Toast(Type type, Component title, Component message, long durationMillis, Runnable onDismiss) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.durationMillis = durationMillis;
        this.onDismiss = onDismiss;
    }

    public static Toast info(String title, String message) { return new Toast(Type.INFO, title, message); }
    public static Toast success(String title, String message) { return new Toast(Type.SUCCESS, title, message); }
    public static Toast warning(String title, String message) { return new Toast(Type.WARNING, title, message); }
    public static Toast error(String title, String message) { return new Toast(Type.ERROR, title, message); }

    public static OverlayHandle show(OverlayManager overlays, Toast toast) {
        if (overlays == null || toast == null) return null;
        return overlays.show(OverlayLayer.TOAST, toast, false, false, false, toast.onDismiss);
    }

    @Override
    public int preferredWidth(Font font) {
        if (font == null) return 160;
        int tw = Math.max(font.width(title), font.width(message));
        return Math.max(160, tw + 28);
    }

    @Override
    public int preferredHeight(Font font) {
        return font != null ? font.lineHeight * 2 + 16 : 32;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        int w = preferredWidth(measureFont());
        int h = preferredHeight(measureFont());
        int tx = availableWidth - w - 10;
        int ty = 10;
        setBounds(tx, ty, w, h);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        if (createdNanos < 0) createdNanos = System.nanoTime();
        long elapsedNanos = System.nanoTime() - createdNanos;
        long durationNanos = durationMillis * 1_000_000L;
        progress = Math.max(0.0F, 1.0F - (float) elapsedNanos / durationNanos);

        int accentCol = switch (type) {
            case INFO -> colors.primary();
            case SUCCESS -> colors.success();
            case WARNING -> colors.warning();
            case ERROR -> colors.danger();
        };

        UiRender.shadow(g, x, y, width, height, t.toastTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.toastTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

        // Status pill on left
        UiRender.roundedRect(g, x + 3, y + 4, 3, height - 8, 1, accentCol);

        // Title and Message
        g.drawString(font, title, x + 12, y + 5, colors.onSurface());
        g.drawString(font, message, x + 12, y + 6 + font.lineHeight, colors.onSurfaceMuted());

        // Progress bar at bottom
        if (progress > 0.0F) {
            int barW = Math.round((width - 6) * progress);
            UiRender.roundedRect(g, x + 3, y + height - 3, barW, 2, 1, accentCol);
        }
    }
}
