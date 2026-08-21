package com.nstut.openui.theme;

public record ColorScheme(
        int backdrop,
        int shell,
        int surface,
        int surfaceRaised,
        int surfaceVariant,
        int input,
        int border,
        int borderSubtle,
        int borderStrong,
        int primary,
        int primaryHover,
        int primaryDim,
        int danger,
        int dangerHover,
        int dangerDeep,
        int warning,
        int warningHover,
        int success,
        int successHover,
        int successDeep,
        int onPrimary,
        int onSurface,
        int onSurfaceMuted,
        int onSurfaceDisabled,
        int highlight,
        int shadow
) {
    public ColorScheme(
            int backdrop, int shell, int surface, int surfaceVariant, int input,
            int border, int borderStrong, int primary, int primaryHover,
            int danger, int warning, int success, int onPrimary, int onSurface, int onSurfaceMuted) {
        this(backdrop, shell, surface, surfaceVariant, surfaceVariant, input,
                border, border, borderStrong, primary, primaryHover, primary,
                danger, danger, danger, warning, warning, success, success, success,
                onPrimary, onSurface, onSurfaceMuted, 0xFF525262, 0x18FFFFFF, 0x40000000);
    }
}

