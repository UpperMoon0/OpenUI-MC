package com.nstut.openui.style;

import java.util.Objects;

/** Lightweight immutable style values layered above theme tokens. */
public record Style(
        Insets margin,
        Insets padding,
        Integer background,
        Integer borderColor,
        int borderWidth,
        int radius,
        Integer width,
        Integer height,
        Integer minWidth,
        Integer minHeight,
        Integer maxWidth,
        Integer maxHeight) {

    public static final Style EMPTY = new Style(Insets.ZERO, Insets.ZERO, null, null, 0, 0,
            null, null, null, null, null, null);

    public Style {
        margin = Objects.requireNonNullElse(margin, Insets.ZERO);
        padding = Objects.requireNonNullElse(padding, Insets.ZERO);
        borderWidth = Math.max(0, borderWidth);
        radius = Math.max(0, radius);
    }

    public static Builder builder() { return new Builder(); }

    /** Values from overlay replace values from this style when explicitly set. */
    public Style merge(Style overlay) {
        Objects.requireNonNull(overlay, "overlay");
        return new Style(
                overlay.margin.equals(Insets.ZERO) ? margin : overlay.margin,
                overlay.padding.equals(Insets.ZERO) ? padding : overlay.padding,
                overlay.background != null ? overlay.background : background,
                overlay.borderColor != null ? overlay.borderColor : borderColor,
                overlay.borderWidth != 0 ? overlay.borderWidth : borderWidth,
                overlay.radius != 0 ? overlay.radius : radius,
                overlay.width != null ? overlay.width : width,
                overlay.height != null ? overlay.height : height,
                overlay.minWidth != null ? overlay.minWidth : minWidth,
                overlay.minHeight != null ? overlay.minHeight : minHeight,
                overlay.maxWidth != null ? overlay.maxWidth : maxWidth,
                overlay.maxHeight != null ? overlay.maxHeight : maxHeight);
    }

    public record Insets(int top, int right, int bottom, int left) {
        public static final Insets ZERO = new Insets(0, 0, 0, 0);
        public Insets {
            if (top < 0 || right < 0 || bottom < 0 || left < 0) {
                throw new IllegalArgumentException("Insets cannot be negative");
            }
        }
        public static Insets all(int value) { return new Insets(value, value, value, value); }
        public static Insets symmetric(int vertical, int horizontal) {
            return new Insets(vertical, horizontal, vertical, horizontal);
        }
    }

    public static final class Builder {
        private Insets margin = Insets.ZERO;
        private Insets padding = Insets.ZERO;
        private Integer background;
        private Integer borderColor;
        private int borderWidth;
        private int radius;
        private Integer width, height, minWidth, minHeight, maxWidth, maxHeight;

        public Builder margin(int value) { margin = Insets.all(value); return this; }
        public Builder margin(Insets value) { margin = value; return this; }
        public Builder padding(int value) { padding = Insets.all(value); return this; }
        public Builder padding(Insets value) { padding = value; return this; }
        public Builder background(int argb) { background = argb; return this; }
        public Builder border(int width, int argb) { borderWidth = width; borderColor = argb; return this; }
        public Builder radius(int value) { radius = value; return this; }
        public Builder width(int value) { width = value; return this; }
        public Builder height(int value) { height = value; return this; }
        public Builder minWidth(int value) { minWidth = value; return this; }
        public Builder minHeight(int value) { minHeight = value; return this; }
        public Builder maxWidth(int value) { maxWidth = value; return this; }
        public Builder maxHeight(int value) { maxHeight = value; return this; }

        public Style build() {
            return new Style(margin, padding, background, borderColor, borderWidth, radius,
                    width, height, minWidth, minHeight, maxWidth, maxHeight);
        }
    }
}
