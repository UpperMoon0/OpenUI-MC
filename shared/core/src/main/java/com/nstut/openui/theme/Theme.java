package com.nstut.openui.theme;

import com.nstut.openui.api.UiTheme;

public record Theme(
        ColorScheme colors,
        Spacing spacing,
        Radii radii,
        Durations durations,
        ButtonTheme buttonTheme,
        TextFieldTheme textFieldTheme,
        CardTheme cardTheme,
        TooltipTheme tooltipTheme,
        DialogTheme dialogTheme,
        TableTheme tableTheme,
        ToastTheme toastTheme,
        boolean reducedMotion
) {
    public Theme(ColorScheme colors, Spacing spacing, Radii radii, Durations durations, boolean reducedMotion) {
        this(colors, spacing, radii, durations,
                ButtonTheme.DEFAULT, TextFieldTheme.DEFAULT, CardTheme.DEFAULT,
                TooltipTheme.DEFAULT, DialogTheme.DEFAULT, TableTheme.DEFAULT, ToastTheme.DEFAULT,
                reducedMotion);
    }

    public static Theme dark() {
        ColorScheme colors = new ColorScheme(
                UiTheme.BACKDROP, UiTheme.SHELL, UiTheme.SURFACE, UiTheme.SURFACE_RAISED, UiTheme.SURFACE_HOVER,
                UiTheme.INPUT, UiTheme.BORDER, UiTheme.BORDER_SUBTLE, UiTheme.BORDER_STRONG,
                UiTheme.ACCENT, UiTheme.ACCENT_HOVER, UiTheme.ACCENT_DIM,
                UiTheme.DANGER, 0xFFE65C5C, UiTheme.DANGER_DEEP,
                UiTheme.WARNING, 0xFFF5B64E,
                UiTheme.SUCCESS, 0xFF52CE85, UiTheme.SUCCESS_DEEP,
                UiTheme.TEXT_PRIMARY, UiTheme.TEXT_PRIMARY, UiTheme.TEXT_MUTED, UiTheme.TEXT_DISABLED,
                UiTheme.HIGHLIGHT, UiTheme.SHADOW
        );
        return new Theme(colors, new Spacing(2, 4, 6, 8, 12), new Radii(3, 5, 7),
                new Durations(120, 90, 150, 180, 200),
                ButtonTheme.DEFAULT, TextFieldTheme.DEFAULT, CardTheme.DEFAULT,
                TooltipTheme.DEFAULT, DialogTheme.DEFAULT, TableTheme.DEFAULT, ToastTheme.DEFAULT,
                false);
    }

    public static Theme light() {
        ColorScheme colors = new ColorScheme(
                0x80000000, 0xFFE2E3E8, 0xFFE9EAEF, 0xFFF0F1F4, 0xFFDCDDE3,
                0xFFF2F3F5, 0xFFC4C6CE, 0xFFD2D4DB, 0xFFA4A7B2,
                0xFF2A50A8, 0xFF3B68D0, 0xFF1F3F86,
                0xFFC62828, 0xFFD32F2F, 0xFF8E1F28,
                0xFF8A5200, 0xFFA66200,
                0xFF2E7D32, 0xFF388E3C, 0xFF1B5E20,
                0xFFFFFFFF, 0xFF1C1C24, 0xFF666678, 0xFFA0A0B0,
                0x20000000, 0x20000000
        );
        return new Theme(colors, new Spacing(2, 4, 6, 8, 12), new Radii(3, 5, 7),
                new Durations(120, 90, 150, 180, 200),
                ButtonTheme.DEFAULT, TextFieldTheme.DEFAULT, CardTheme.DEFAULT,
                TooltipTheme.DEFAULT, DialogTheme.DEFAULT, TableTheme.DEFAULT, ToastTheme.DEFAULT,
                false);
    }

    public static Theme highContrast() {
        ColorScheme colors = new ColorScheme(
                0xD0000000, 0xFF000000, 0xFF000000, 0xFF111111, 0xFF222222,
                0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                0xFFFFFF00, 0xFFFFFF55, 0xFFAAAA00,
                0xFFFF5555, 0xFFFF7777, 0xFFAA0000,
                0xFFFFAA00, 0xFFFFCC00,
                0xFF55FF55, 0xFF77FF77, 0xFF00AA00,
                0xFF000000, 0xFFFFFFFF, 0xFFCCCCCC, 0xFF888888,
                0x40FFFFFF, 0x80000000
        );
        return new Theme(colors, new Spacing(2, 4, 6, 8, 12), new Radii(2, 4, 6),
                new Durations(0, 0, 0, 0, 0),
                ButtonTheme.DEFAULT, TextFieldTheme.DEFAULT, CardTheme.DEFAULT,
                TooltipTheme.DEFAULT, DialogTheme.DEFAULT, TableTheme.DEFAULT, ToastTheme.DEFAULT,
                true);
    }

    public Theme withReducedMotion(boolean reducedMotion) {
        return new Theme(colors, spacing, radii, durations,
                buttonTheme, textFieldTheme, cardTheme, tooltipTheme, dialogTheme, tableTheme, toastTheme,
                reducedMotion);
    }

    public record Spacing(int xs, int sm, int md, int lg, int xl) { }
    public record Radii(int small, int medium, int large) { }
    public record Durations(int hoverMs, int pressMs, int tooltipMs, int overlayMs, int pageMs) { }
    public record ButtonTheme(int radius, int heightSm, int heightMd, int heightLg) {
        public static final ButtonTheme DEFAULT = new ButtonTheme(3, 12, 14, 20);
    }
    public record TextFieldTheme(int radius, int paddingH, int paddingV) {
        public static final TextFieldTheme DEFAULT = new TextFieldTheme(3, 4, 2);
    }
    public record CardTheme(int radius, int padding, boolean elevated) {
        public static final CardTheme DEFAULT = new CardTheme(5, 8, true);
    }
    public record TooltipTheme(int radius, int paddingH, int paddingV) {
        public static final TooltipTheme DEFAULT = new TooltipTheme(3, 6, 4);
    }
    public record DialogTheme(int radius, int padding, int minWidth, int minHeight) {
        public static final DialogTheme DEFAULT = new DialogTheme(6, 12, 220, 90);
    }
    public record TableTheme(int rowHeight, int headerHeight) {
        public static final TableTheme DEFAULT = new TableTheme(18, 18);
    }
    public record ToastTheme(int radius, int padding, int durationMs) {
        public static final ToastTheme DEFAULT = new ToastTheme(4, 8, 3000);
    }
}

