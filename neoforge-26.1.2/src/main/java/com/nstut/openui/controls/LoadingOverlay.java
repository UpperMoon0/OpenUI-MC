package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class LoadingOverlay extends UIComponent {
    private final UIComponent content;
    private final Spinner spinner = new Spinner(20);
    private Component message;
    private boolean loading;

    public LoadingOverlay(UIComponent content) {
        this.content = content;
        if (content != null) addChild(content);
        addChild(spinner);
    }

    public LoadingOverlay loading(boolean loading) {
        this.loading = loading;
        invalidatePaint();
        return this;
    }

    public LoadingOverlay message(String message) {
        this.message = Component.literal(message != null ? message : "");
        invalidatePaint();
        return this;
    }

    public LoadingOverlay message(Component message) {
        this.message = message;
        invalidatePaint();
        return this;
    }

    @Override
    public int preferredWidth(Font font) {
        return content != null ? content.preferredWidth(font) : 60;
    }

    @Override
    public int preferredHeight(Font font) {
        return content != null ? content.preferredHeight(font) : 60;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        if (content != null) {
            content.layoutTree(measureFont(), lx, ly, availableWidth, availableHeight);
        }
        spinner.layoutTree(measureFont(), lx + (availableWidth - 20) / 2, ly + (availableHeight - 20) / 2, 20, 20);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (content != null) {
            content.render(g, font, mx, my, pt);
        }
        if (loading) {
            Theme t = theme();
            ColorScheme colors = t.colors();
            g.fill(x, y, x + width, y + height, colors.backdrop());
            spinner.render(g, font, mx, my, pt);
            if (message != null) {
                int mw = font.width(message);
                g.text(font, message, x + (width - mw) / 2, y + (height - 20) / 2 + 26, colors.onSurface());
            }
        }
    }

    @Override
    public UIComponent hitTest(int mx, int my) {
        if (loading && visible && mx >= x && mx < x + width && my >= y && my < y + height) {
            return this; // Block clicks to children when loading
        }
        return super.hitTest(mx, my);
    }
}
