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

public class AreaChart extends UIComponent {
    public record DataPoint(String label, double value) {}

    private final List<DataPoint> data = new ArrayList<>();
    private Integer areaColor;
    private boolean showGrid = true;
    private Function<Double, String> valueFormatter = v -> String.format("%.1f", v);
    private int hoveredIndex = -1;

    public AreaChart() {}

    public AreaChart(List<Double> values) {
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                data.add(new DataPoint(String.valueOf(i), values.get(i)));
            }
        }
    }

    public AreaChart point(String label, double value) {
        data.add(new DataPoint(label, value));
        invalidatePaint();
        return this;
    }

    public AreaChart color(int color) {
        this.areaColor = color;
        invalidatePaint();
        return this;
    }

    public AreaChart formatter(Function<Double, String> formatter) {
        this.valueFormatter = Objects.requireNonNull(formatter);
        return this;
    }

    public AreaChart showGrid(boolean show) {
        this.showGrid = show;
        return this;
    }

    @Override
    public int preferredWidth(Font font) { return 180; }

    @Override
    public int preferredHeight(Font font) { return 100; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int chartColor = areaColor != null ? areaColor : colors.primary();

        UiRender.roundedOutline(g, x, y, width, height, t.radii().small(), colors.surface(), colors.border());

        if (data.size() < 2) {
            if (font != null) {
                g.text(font, "No Data", x + (width - font.width("No Data")) / 2, y + (height - font.lineHeight) / 2, colors.onSurfaceMuted());
            }
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (DataPoint dp : data) {
            min = Math.min(min, dp.value());
            max = Math.max(max, dp.value());
        }
        if (Math.abs(max - min) < 0.0001) {
            min -= 1;
            max += 1;
        }

        int padding = 10;
        int plotX = x + padding;
        int plotY = y + padding;
        int plotW = width - padding * 2;
        int plotH = height - padding * 2;

        if (showGrid) {
            for (int i = 0; i <= 3; i++) {
                int gy = plotY + i * plotH / 3;
                g.fill(plotX, gy, plotX + plotW, gy + 1, colors.borderSubtle());
            }
        }

        int count = data.size();
        int[] px = new int[count];
        int[] py = new int[count];

        for (int i = 0; i < count; i++) {
            px[i] = plotX + i * plotW / (count - 1);
            double normalized = (data.get(i).value() - min) / (max - min);
            py[i] = plotY + plotH - (int) Math.round(normalized * plotH);
        }

        // Fill area under curves
        int fillAlpha = (chartColor & 0x00FFFFFF) | 0x40000000;
        for (int i = 0; i < count - 1; i++) {
            int x0 = px[i];
            int x1 = px[i + 1];
            int y0 = py[i];
            int y1 = py[i + 1];

            // Fill vertical slices
            for (int col = x0; col <= x1; col++) {
                float progress = (x1 == x0) ? 0 : (float) (col - x0) / (x1 - x0);
                int topY = Math.round(y0 + progress * (y1 - y0));
                g.fill(col, topY, col + 1, plotY + plotH, fillAlpha);
            }
            drawLine(g, x0, y0, x1, y1, chartColor);
        }

        // Find hovered point
        hoveredIndex = -1;
        for (int i = 0; i < count; i++) {
            int dx = mx - px[i];
            int dy = my - py[i];
            if (dx * dx + dy * dy <= 25) {
                hoveredIndex = i;
                break;
            }
        }

        // Render point dots
        for (int i = 0; i < count; i++) {
            boolean isPointHovered = (i == hoveredIndex);
            int r = isPointHovered ? 3 : 2;
            int dotCol = isPointHovered ? colors.onPrimary() : chartColor;
            g.fill(px[i] - r, py[i] - r, px[i] + r + 1, py[i] + r + 1, dotCol);
        }

        // Render hover tooltip
        if (hoveredIndex >= 0 && font != null) {
            DataPoint dp = data.get(hoveredIndex);
            String label = dp.label() + ": " + valueFormatter.apply(dp.value());
            int tw = font.width(label);
            int tipX = Math.max(x + 2, Math.min(x + width - tw - 8, px[hoveredIndex] - tw / 2));
            int tipY = Math.max(y + 2, py[hoveredIndex] - 16);

            UiRender.roundedOutline(g, tipX - 2, tipY - 2, tw + 4, font.lineHeight + 4, 2, colors.surfaceRaised(), colors.borderStrong());
            g.text(font, label, tipX, tipY, colors.onSurface());
        }
    }

    private void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }
}
