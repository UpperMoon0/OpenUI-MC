package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;
import java.util.function.Supplier;

public class ErrorBoundary extends UIComponent {
    private final Supplier<UIComponent> childSupplier;
    private UIComponent currentChild;
    private Throwable caughtError;

    public ErrorBoundary(Supplier<UIComponent> childSupplier) {
        this.childSupplier = Objects.requireNonNull(childSupplier);
        rebuild();
    }

    public void rebuild() {
        clearChildren();
        caughtError = null;
        try {
            currentChild = childSupplier.get();
            if (currentChild != null) addChild(currentChild);
        } catch (Throwable t) {
            caughtError = t;
        }
        invalidateBuild();
    }

    @Override
    public int preferredWidth(Font font) {
        if (caughtError != null || currentChild == null) return 140;
        try {
            return currentChild.preferredWidth(font);
        } catch (Throwable t) {
            caughtError = t;
            return 140;
        }
    }

    @Override
    public int preferredHeight(Font font) {
        if (caughtError != null || currentChild == null) return 60;
        try {
            return currentChild.preferredHeight(font);
        } catch (Throwable t) {
            caughtError = t;
            return 60;
        }
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        if (caughtError == null && currentChild != null) {
            try {
                currentChild.layoutTree(measureFont(), lx, ly, availableWidth, availableHeight);
            } catch (Throwable t) {
                caughtError = t;
            }
        }
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (caughtError != null) {
            Theme t = theme();
            ColorScheme colors = t.colors();

            UiRender.roundedOutline(g, x, y, width, height, t.radii().small(), colors.dangerDeep(), colors.danger());
            g.drawString(font, "Component Error", x + 8, y + 6, colors.danger());
            String msg = caughtError.getMessage() != null ? caughtError.getMessage() : caughtError.getClass().getSimpleName();
            g.drawString(font, msg, x + 8, y + 8 + font.lineHeight, colors.onSurfaceMuted());
            return;
        }

        if (currentChild != null) {
            try {
                currentChild.render(g, font, mx, my, pt);
            } catch (Throwable t) {
                caughtError = t;
                invalidatePaint();
            }
        }
    }
}
