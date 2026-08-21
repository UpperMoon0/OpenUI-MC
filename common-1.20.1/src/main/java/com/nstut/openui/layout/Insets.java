package com.nstut.openui.layout;

public record Insets(int top, int right, int bottom, int left) {
    public static final Insets ZERO = all(0);

    public Insets {
        top = Math.max(0, top);
        right = Math.max(0, right);
        bottom = Math.max(0, bottom);
        left = Math.max(0, left);
    }

    public static Insets all(int value) { return new Insets(value, value, value, value); }
    public static Insets symmetric(int vertical, int horizontal) { return new Insets(vertical, horizontal, vertical, horizontal); }
    public static Insets only(int top, int right, int bottom, int left) { return new Insets(top, right, bottom, left); }
    public int horizontal() { return left + right; }
    public int vertical() { return top + bottom; }
}

