package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class Sparkline extends UIComponent {
    private final List<Double> values = new ArrayList<>();
    private Integer lineColor;
    private int reqWidth = 50;
    private int reqHeight = 16;

    public Sparkline() {}

    public Sparkline(List<Double> data) {
        if (data != null) values.addAll(data);
    }

    public Sparkline value(double v) {
        values.add(v);
        invalidatePaint();
        return this;
    }

    public Sparkline size(int width, int height) {
        this.reqWidth = Math.max(10, width);
        this.reqHeight = Math.max(8, height);
        invalidateLayout();
        return this;
    }

    public Sparkline color(int color) {
        this.lineColor = color;
        invalidatePaint();
        return this;
    }

    @Override
    public int preferredWidth(Font font) { return reqWidth; }

    @Override
    public int preferredHeight(Font font) { return reqHeight; }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible || values.size() < 2) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int col = lineColor != null ? lineColor : colors.primary();

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (Math.abs(max - min) < 0.0001) {
            min -= 1;
            max += 1;
        }

        int count = values.size();
        int[] px = new int[count];
        int[] py = new int[count];

        for (int i = 0; i < count; i++) {
            px[i] = x + i * (width - 1) / (count - 1);
            double normalized = (values.get(i) - min) / (max - min);
            py[i] = y + height - 1 - (int) Math.round(normalized * (height - 2));
        }

        for (int i = 0; i < count - 1; i++) {
            drawLine(g, px[i], py[i], px[i + 1], py[i + 1], col);
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
