package com.nstut.openui.theme;

public record TextStyle(
        Role role,
        float scale,
        boolean shadow,
        boolean bold,
        boolean italic,
        Integer colorOverride
) {
    public enum Role {
        DISPLAY,
        TITLE,
        HEADING,
        BODY,
        LABEL,
        CAPTION,
        MONO
    }

    public static final TextStyle DISPLAY = new TextStyle(Role.DISPLAY, 1.5F, true, true, false, null);
    public static final TextStyle TITLE = new TextStyle(Role.TITLE, 1.25F, true, true, false, null);
    public static final TextStyle HEADING = new TextStyle(Role.HEADING, 1.1F, true, true, false, null);
    public static final TextStyle BODY = new TextStyle(Role.BODY, 1.0F, true, false, false, null);
    public static final TextStyle LABEL = new TextStyle(Role.LABEL, 1.0F, false, false, false, null);
    public static final TextStyle CAPTION = new TextStyle(Role.CAPTION, 0.85F, false, false, false, null);
    public static final TextStyle MONO = new TextStyle(Role.MONO, 1.0F, false, false, false, null);

    public TextStyle withColor(int color) {
        return new TextStyle(role, scale, shadow, bold, italic, color);
    }

    public TextStyle withScale(float scale) {
        return new TextStyle(role, scale, shadow, bold, italic, colorOverride);
    }

    public TextStyle withShadow(boolean shadow) {
        return new TextStyle(role, scale, shadow, bold, italic, colorOverride);
    }

    public TextStyle withBold(boolean bold) {
        return new TextStyle(role, scale, shadow, bold, italic, colorOverride);
    }

    public TextStyle withItalic(boolean italic) {
        return new TextStyle(role, scale, shadow, bold, italic, colorOverride);
    }
}
