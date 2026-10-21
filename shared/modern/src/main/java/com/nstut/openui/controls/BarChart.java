package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class BarChart extends UIComponent {
    public record BarItem(String label, double value, Integer customColor) {
        public BarItem(String label, double value) {
            this(label, value, null);
        }
    }

    private final List<BarItem> items = new ArrayList<>();
    private Function<Double, String> formatter = v -> String.format("%.0f", v);
    private int hoveredBar = -1;

    public BarChart() {}

    public BarChart bar(String label, double value) {
        items.add(new BarItem(label, value));
        invalidatePaint();
        return this;
    }

    public BarChart bar(String label, double value, int color) {
        items.add(new BarItem(label, value, color));
        invalidatePaint();
        return this;
    }

    public BarChart formatter(Function<Double, String> formatter) {
        this.formatter = Objects.requireNonNull(formatter);
        return this;
    }

    @Override
    public int preferredWidth(Font font) { return Math.max(120, items.size() * 30 + 20); }

    @Override
    public int preferredHeight(Font font) { return 90; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.roundedOutline(g, x, y, width, height, t.radii().small(), colors.surface(), colors.border());

        if (items.isEmpty()) return;

        double max = 0.0001;
        for (BarItem bi : items) max = Math.max(max, bi.value());

        int pad = 8;
        int plotX = x + pad;
        int plotY = y + pad;
        int plotW = width - pad * 2;
        int plotH = height - pad * 2 - font.lineHeight - 2;

        int count = items.size();
        int slotW = plotW / count;
        int barW = Math.max(4, slotW - 6);

        hoveredBar = -1;
        for (int i = 0; i < count; i++) {
            BarItem item = items.get(i);
            int barH = (int) Math.round((item.value() / max) * plotH);
            int bx = plotX + i * slotW + (slotW - barW) / 2;
            int by = plotY + plotH - barH;

            boolean isHovered = mx >= bx && mx < bx + barW && my >= by && my < plotY + plotH;
            if (isHovered) hoveredBar = i;

            int col = item.customColor() != null ? item.customColor()
                    : (isHovered ? colors.primaryHover() : colors.primary());

            UiRender.roundedRect(g, bx, by, barW, barH, 2, col);

            // Label
            int labelW = font.width(item.label());
            int lx = plotX + i * slotW + (slotW - labelW) / 2;
            int ly = plotY + plotH + 3;
            g.text(font, item.label(), lx, ly, colors.onSurfaceMuted(), false);
        }

        if (hoveredBar >= 0) {
            BarItem item = items.get(hoveredBar);
            String valStr = formatter.apply(item.value());
            int tw = font.width(valStr);
            int tipX = Math.max(x + 2, Math.min(x + width - tw - 6, mx - tw / 2));
            int tipY = Math.max(y + 2, my - 14);

            UiRender.roundedOutline(g, tipX - 2, tipY - 2, tw + 4, font.lineHeight + 4, 2, colors.surfaceRaised(), colors.borderStrong());
            g.text(font, valStr, tipX, tipY, colors.onSurface(), false);
        }
    }
}
