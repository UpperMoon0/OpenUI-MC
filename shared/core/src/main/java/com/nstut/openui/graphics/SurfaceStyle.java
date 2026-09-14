package com.nstut.openui.graphics;

/** Immutable, renderer-neutral tinted surface. Colors use straight ARGB. */
public record SurfaceStyle(int topColor, int bottomColor, int borderColor,
        int radius, int shadowExtent, int shadowColor) {
    public SurfaceStyle {
        radius = Math.max(0, radius);
        shadowExtent = Math.max(0, Math.min(16, shadowExtent));
    }
}
