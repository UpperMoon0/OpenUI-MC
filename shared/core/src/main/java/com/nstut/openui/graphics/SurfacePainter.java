package com.nstut.openui.graphics;

/** Scanline surfaces with disjoint fill/border pixels: translucent colors never self-overlap. */
public final class SurfacePainter {
    private SurfacePainter() { }
    @FunctionalInterface public interface Fill { void draw(int x, int y, int width, int height, int color); }

    public static void paint(Fill fill, int x, int y, int width, int height, SurfaceStyle style) {
        if (width <= 0 || height <= 0) return;
        if (width <= 2 || height <= 2) {
            fill.draw(x, y, width, height, style.borderColor());
            return;
        }
        int radius = Math.min(style.radius(), Math.min(width, height) / 2);
        int extent = style.shadowExtent();
        // Disjoint one-pixel rings give a soft falloff without repeated alpha overdraw.
        for (int ring = extent; ring > 0; ring--) {
            int alpha = (style.shadowColor() >>> 24) * (extent - ring + 1) / (extent + 1);
            outline(fill, x - ring, y - ring + 2, width + ring * 2, height + ring * 2,
                    radius + ring, alpha << 24 | style.shadowColor() & 0xFFFFFF);
        }
        for (int row = 0; row < height; row++) {
            int inset = inset(row, width, height, radius);
            int color = mix(style.topColor(), style.bottomColor(), height == 1 ? 0 : (float) row / (height - 1));
            int innerInset = row == 0 || row == height - 1 ? width / 2 :
                    1 + inset(row - 1, width - 2, height - 2, Math.max(0, radius - 1));
            if (row == 0 || row == height - 1) {
                fill.draw(x + inset, y + row, width - inset * 2, 1, style.borderColor());
            } else {
                int edge = Math.max(0, innerInset - inset);
                if (edge > 0) {
                    fill.draw(x + inset, y + row, edge, 1, style.borderColor());
                    fill.draw(x + width - innerInset, y + row, edge, 1, style.borderColor());
                }
                if (width > innerInset * 2) fill.draw(x + innerInset, y + row, width - innerInset * 2, 1, color);
            }
        }
    }

    private static void outline(Fill f, int x, int y, int w, int h, int r, int color) {
        for (int row = 0; row < h; row++) {
            int outer = inset(row, w, h, r);
            if (row == 0 || row == h - 1) f.draw(x + outer, y + row, w - 2 * outer, 1, color);
            else {
                int inner = 1 + inset(row - 1, w - 2, h - 2, Math.max(0, r - 1));
                if (inner > outer) {
                    f.draw(x + outer, y + row, inner - outer, 1, color);
                    f.draw(x + w - inner, y + row, inner - outer, 1, color);
                }
            }
        }
    }

    private static int inset(int row, int width, int height, int radius) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r <= 1) return 0;
        int edge = Math.min(row, height - row - 1);
        if (edge >= r) return 0;
        double dy = r - edge - .5;
        return Math.max(0, (int) Math.ceil(r - Math.sqrt(r * r - dy * dy)));
    }

    private static int mix(int a, int b, float t) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int start = a >>> shift & 255;
            result |= (start + Math.round(((b >>> shift & 255) - start) * t)) << shift;
        }
        return result;
    }
}
