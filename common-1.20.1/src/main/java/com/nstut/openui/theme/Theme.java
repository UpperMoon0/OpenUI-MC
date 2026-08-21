package com.nstut.openui.theme;

import com.nstut.openui.api.UiTheme;

public record Theme(ColorScheme colors, Spacing spacing, Radii radii, Durations durations, boolean reducedMotion) {
    public static Theme dark() {
        return new Theme(new ColorScheme(
                UiTheme.BACKDROP, UiTheme.SHELL, UiTheme.SURFACE, UiTheme.SURFACE_RAISED, UiTheme.INPUT,
                UiTheme.BORDER, UiTheme.BORDER_STRONG, UiTheme.ACCENT, UiTheme.ACCENT_HOVER,
                UiTheme.DANGER, UiTheme.WARNING, UiTheme.SUCCESS, UiTheme.SHELL,
                UiTheme.TEXT_PRIMARY, UiTheme.TEXT_MUTED),
                new Spacing(2, 4, 6, 8, 12), new Radii(3, 5, 7),
                new Durations(120, 90, 150, 180, 200), false);
    }

    public Theme withReducedMotion(boolean reducedMotion) {
        return new Theme(colors, spacing, radii, durations, reducedMotion);
    }

    public record Spacing(int xs, int sm, int md, int lg, int xl) { }
    public record Radii(int small, int medium, int large) { }
    public record Durations(int hoverMs, int pressMs, int tooltipMs, int overlayMs, int pageMs) { }
}

