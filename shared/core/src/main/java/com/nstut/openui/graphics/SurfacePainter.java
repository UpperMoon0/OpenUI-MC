package com.nstut.openui.graphics;

/** Scanline surfaces with disjoint fill/border pixels: translucent colors never self-overlap. */
public final class SurfacePainter {
    private static final int MAX_GRADIENT_BANDS = 64;

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
        // Each shadow band is a disjoint rounded ring. Straight edges coalesce into tall rectangles,
        // so render-state count depends on corner radius/extent rather than panel height * extent.
        for (int ring = extent; ring > 0; ring--) {
            int alpha = (style.shadowColor() >>> 24) * (extent - ring + 1) / (extent + 1);
            int color = alpha << 24 | style.shadowColor() & 0xFFFFFF;
            RoundedGeometry.paintRing(fill::draw, x - ring, y - ring + 2,
                    width + ring * 2, height + ring * 2, radius + ring, 1, color);
        }

        RoundedGeometry.paintRing(fill::draw, x, y, width, height, radius, 1, style.borderColor());
        int innerWidth = width - 2;
        int innerHeight = height - 2;
        int innerRadius = Math.max(0, radius - 1);
        if (style.topColor() == style.bottomColor()) {
            RoundedGeometry.paintRounded(fill::draw, x + 1, y + 1,
                    innerWidth, innerHeight, innerRadius, style.topColor());
            return;
        }
        int bands = Math.min(MAX_GRADIENT_BANDS, innerHeight);
        int runX = 0, runY = 0, runWidth = 0, runHeight = 0, runColor = 0;
        for (int row = 0; row < innerHeight; row++) {
            int inset = RoundedGeometry.inset(row, innerWidth, innerHeight, innerRadius);
            int band = Math.min(bands - 1, row * bands / innerHeight);
            float bandCenterRow = ((band + 0.5F) * innerHeight / bands) + 1.0F;
            int color = mix(style.topColor(), style.bottomColor(), bandCenterRow / (height - 1));
            int span = innerWidth - inset * 2;
            int rowX = x + 1 + inset;
            int rowY = y + 1 + row;
            if (span > 0 && runHeight > 0 && rowX == runX && span == runWidth
                    && color == runColor && runY + runHeight == rowY) {
                runHeight++;
                continue;
            }
            if (runHeight > 0) fill.draw(runX, runY, runWidth, runHeight, runColor);
            if (span > 0) {
                runX = rowX;
                runY = rowY;
                runWidth = span;
                runHeight = 1;
                runColor = color;
            } else {
                runHeight = 0;
            }
        }
        if (runHeight > 0) fill.draw(runX, runY, runWidth, runHeight, runColor);
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
