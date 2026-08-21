package com.nstut.openui.layout;

public record Constraints(int minWidth, int maxWidth, int minHeight, int maxHeight) {
    public static final int INFINITY = 1_000_000;

    public Constraints {
        minWidth = Math.max(0, minWidth);
        minHeight = Math.max(0, minHeight);
        maxWidth = Math.max(minWidth, maxWidth);
        maxHeight = Math.max(minHeight, maxHeight);
    }

    public static Constraints tight(int width, int height) {
        return new Constraints(width, width, height, height);
    }

    public static Constraints loose(int maxWidth, int maxHeight) {
        return new Constraints(0, maxWidth, 0, maxHeight);
    }

    public int constrainWidth(int width) { return Math.max(minWidth, Math.min(maxWidth, width)); }
    public int constrainHeight(int height) { return Math.max(minHeight, Math.min(maxHeight, height)); }
    public Size constrain(Size size) { return new Size(constrainWidth(size.width()), constrainHeight(size.height())); }

    public Constraints inset(Insets insets) {
        int horizontal = insets.horizontal();
        int vertical = insets.vertical();
        return new Constraints(
                Math.max(0, minWidth - horizontal), Math.max(0, maxWidth - horizontal),
                Math.max(0, minHeight - vertical), Math.max(0, maxHeight - vertical));
    }
}

