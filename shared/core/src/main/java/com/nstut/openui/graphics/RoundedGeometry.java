package com.nstut.openui.graphics;

/** Shared rounded-rectangle geometry emitted as non-overlapping rectangular spans. */
final class RoundedGeometry {
    private RoundedGeometry() { }

    @FunctionalInterface
    interface Fill {
        void draw(int x, int y, int width, int height, int color);
    }

    static void paintRounded(Fill fill, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) return;
        Run run = new Run();
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        for (int row = 0; row < height; row++) {
            int inset = inset(row, width, height, r);
            run.accept(fill, x + inset, y + row, width - inset * 2, color);
        }
        run.flush(fill, color);
    }

    static void paintRing(
            Fill fill,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int thickness,
            int color) {
        if (width <= 0 || height <= 0 || thickness <= 0) return;
        int t = Math.min(thickness, Math.min(width, height) / 2);
        if (t <= 0) return;

        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        int innerWidth = width - t * 2;
        int innerHeight = height - t * 2;
        if (innerWidth <= 0 || innerHeight <= 0) {
            paintRounded(fill, x, y, width, height, r, color);
            return;
        }

        int innerRadius = Math.max(0, r - t);
        Run first = new Run();
        Run second = new Run();
        for (int row = 0; row < height; row++) {
            int outerInset = inset(row, width, height, r);
            int outerLeft = x + outerInset;
            int outerRight = x + width - outerInset;

            if (row < t || row >= height - t) {
                first.accept(fill, outerLeft, y + row, outerRight - outerLeft, color);
                second.accept(fill, 0, y + row, 0, color);
                continue;
            }

            int innerInset = inset(row - t, innerWidth, innerHeight, innerRadius);
            int innerLeft = x + t + innerInset;
            int innerRight = x + width - t - innerInset;
            if (innerLeft >= innerRight) {
                first.accept(fill, outerLeft, y + row, outerRight - outerLeft, color);
                second.accept(fill, 0, y + row, 0, color);
            } else {
                first.accept(fill, outerLeft, y + row, Math.max(0, innerLeft - outerLeft), color);
                second.accept(fill, innerRight, y + row, Math.max(0, outerRight - innerRight), color);
            }
        }
        first.flush(fill, color);
        second.flush(fill, color);
    }

    static int inset(int row, int width, int height, int radius) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r <= 1) return 0;
        int edge = Math.min(row, height - row - 1);
        if (edge >= r) return 0;
        double dy = r - edge - .5;
        return Math.max(0, (int) Math.ceil(r - Math.sqrt(r * r - dy * dy)));
    }

    private static final class Run {
        private int x;
        private int y;
        private int width;
        private int height;

        void accept(Fill fill, int nextX, int nextY, int nextWidth, int color) {
            if (nextWidth <= 0) {
                flush(fill, color);
                return;
            }
            if (height > 0 && x == nextX && width == nextWidth && y + height == nextY) {
                height++;
                return;
            }
            flush(fill, color);
            x = nextX;
            y = nextY;
            width = nextWidth;
            height = 1;
        }

        void flush(Fill fill, int color) {
            if (height > 0) fill.draw(x, y, width, height, color);
            height = 0;
        }
    }
}
