package com.nstut.openui.style;

import com.nstut.openui.theme.TextStyle;

import java.util.EnumSet;
import java.util.Objects;

/** Lightweight immutable style values layered above theme tokens. */
public final class Style {
    private enum Property {
        MARGIN, PADDING, BACKGROUND, BORDER_COLOR, BORDER_WIDTH, RADIUS,
        WIDTH, HEIGHT, MIN_WIDTH, MIN_HEIGHT, MAX_WIDTH, MAX_HEIGHT, TYPOGRAPHY
    }

    public static final Style EMPTY = new Style(
            Insets.ZERO, Insets.ZERO, null, null, 0, 0,
            null, null, null, null, null, null, null,
            EnumSet.noneOf(Property.class));

    private final Insets margin;
    private final Insets padding;
    private final Integer background;
    private final Integer borderColor;
    private final int borderWidth;
    private final int radius;
    private final Integer width;
    private final Integer height;
    private final Integer minWidth;
    private final Integer minHeight;
    private final Integer maxWidth;
    private final Integer maxHeight;
    private final TextStyle typography;
    private final EnumSet<Property> explicit;

    /** Backward-compatible value constructor. Prefer {@link #builder()}. */
    public Style(
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
        this(
                Objects.requireNonNullElse(margin, Insets.ZERO),
                Objects.requireNonNullElse(padding, Insets.ZERO),
                background,
                borderColor,
                Math.max(0, borderWidth),
                Math.max(0, radius),
                width, height, minWidth, minHeight, maxWidth, maxHeight, null,
                inferExplicit(margin, padding, background, borderColor, borderWidth, radius,
                        width, height, minWidth, minHeight, maxWidth, maxHeight));
    }

    private Style(
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
            Integer maxHeight,
            TextStyle typography,
            EnumSet<Property> explicit) {
        this.margin = Objects.requireNonNull(margin, "margin");
        this.padding = Objects.requireNonNull(padding, "padding");
        this.background = background;
        this.borderColor = borderColor;
        this.borderWidth = Math.max(0, borderWidth);
        this.radius = Math.max(0, radius);
        this.width = dimension(width, "width");
        this.height = dimension(height, "height");
        this.minWidth = dimension(minWidth, "minWidth");
        this.minHeight = dimension(minHeight, "minHeight");
        this.maxWidth = dimension(maxWidth, "maxWidth");
        this.maxHeight = dimension(maxHeight, "maxHeight");
        this.typography = typography;
        this.explicit = explicit.clone();
    }

    public static Builder builder() { return new Builder(); }

    public Insets margin() { return margin; }
    public Insets padding() { return padding; }
    public Integer background() { return background; }
    public Integer borderColor() { return borderColor; }
    public int borderWidth() { return borderWidth; }
    public int radius() { return radius; }
    public Integer width() { return width; }
    public Integer height() { return height; }
    public Integer minWidth() { return minWidth; }
    public Integer minHeight() { return minHeight; }
    public Integer maxWidth() { return maxWidth; }
    public Integer maxHeight() { return maxHeight; }
    public TextStyle typography() { return typography; }

    /** Values explicitly set on {@code overlay} replace this style, including zero/none values. */
    public Style merge(Style overlay) {
        Objects.requireNonNull(overlay, "overlay");
        EnumSet<Property> mergedExplicit = explicit.clone();
        mergedExplicit.addAll(overlay.explicit);
        return new Style(
                overlay.has(Property.MARGIN) ? overlay.margin : margin,
                overlay.has(Property.PADDING) ? overlay.padding : padding,
                overlay.has(Property.BACKGROUND) ? overlay.background : background,
                overlay.has(Property.BORDER_COLOR) ? overlay.borderColor : borderColor,
                overlay.has(Property.BORDER_WIDTH) ? overlay.borderWidth : borderWidth,
                overlay.has(Property.RADIUS) ? overlay.radius : radius,
                overlay.has(Property.WIDTH) ? overlay.width : width,
                overlay.has(Property.HEIGHT) ? overlay.height : height,
                overlay.has(Property.MIN_WIDTH) ? overlay.minWidth : minWidth,
                overlay.has(Property.MIN_HEIGHT) ? overlay.minHeight : minHeight,
                overlay.has(Property.MAX_WIDTH) ? overlay.maxWidth : maxWidth,
                overlay.has(Property.MAX_HEIGHT) ? overlay.maxHeight : maxHeight,
                overlay.has(Property.TYPOGRAPHY) ? overlay.typography : typography,
                mergedExplicit);
    }

    private boolean has(Property property) { return explicit.contains(property); }

    private static Integer dimension(Integer value, String name) {
        if (value != null && value < 0) throw new IllegalArgumentException(name + " cannot be negative");
        return value;
    }

    private static EnumSet<Property> inferExplicit(
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
        EnumSet<Property> set = EnumSet.noneOf(Property.class);
        if (margin != null && !margin.equals(Insets.ZERO)) set.add(Property.MARGIN);
        if (padding != null && !padding.equals(Insets.ZERO)) set.add(Property.PADDING);
        if (background != null) set.add(Property.BACKGROUND);
        if (borderColor != null) set.add(Property.BORDER_COLOR);
        if (borderWidth != 0) set.add(Property.BORDER_WIDTH);
        if (radius != 0) set.add(Property.RADIUS);
        if (width != null) set.add(Property.WIDTH);
        if (height != null) set.add(Property.HEIGHT);
        if (minWidth != null) set.add(Property.MIN_WIDTH);
        if (minHeight != null) set.add(Property.MIN_HEIGHT);
        if (maxWidth != null) set.add(Property.MAX_WIDTH);
        if (maxHeight != null) set.add(Property.MAX_HEIGHT);
        return set;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Style style)) return false;
        return borderWidth == style.borderWidth
                && radius == style.radius
                && margin.equals(style.margin)
                && padding.equals(style.padding)
                && Objects.equals(background, style.background)
                && Objects.equals(borderColor, style.borderColor)
                && Objects.equals(width, style.width)
                && Objects.equals(height, style.height)
                && Objects.equals(minWidth, style.minWidth)
                && Objects.equals(minHeight, style.minHeight)
                && Objects.equals(maxWidth, style.maxWidth)
                && Objects.equals(maxHeight, style.maxHeight)
                && Objects.equals(typography, style.typography)
                && explicit.equals(style.explicit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(margin, padding, background, borderColor, borderWidth, radius,
                width, height, minWidth, minHeight, maxWidth, maxHeight, typography, explicit);
    }

    @Override
    public String toString() {
        return "Style[margin=" + margin + ", padding=" + padding + ", background=" + background
                + ", borderColor=" + borderColor + ", borderWidth=" + borderWidth + ", radius=" + radius
                + ", width=" + width + ", height=" + height + ", minWidth=" + minWidth
                + ", minHeight=" + minHeight + ", maxWidth=" + maxWidth + ", maxHeight=" + maxHeight
                + ", typography=" + typography + ']';
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
        private TextStyle typography;
        private final EnumSet<Property> explicit = EnumSet.noneOf(Property.class);

        public Builder margin(int value) { return margin(Insets.all(value)); }
        public Builder margin(Insets value) { margin = Objects.requireNonNull(value); explicit.add(Property.MARGIN); return this; }
        public Builder padding(int value) { return padding(Insets.all(value)); }
        public Builder padding(Insets value) { padding = Objects.requireNonNull(value); explicit.add(Property.PADDING); return this; }
        public Builder background(int argb) { background = argb; explicit.add(Property.BACKGROUND); return this; }
        public Builder noBackground() { background = null; explicit.add(Property.BACKGROUND); return this; }
        public Builder border(int width, int argb) {
            if (width < 0) throw new IllegalArgumentException("border width cannot be negative");
            borderWidth = width; borderColor = argb;
            explicit.add(Property.BORDER_WIDTH); explicit.add(Property.BORDER_COLOR);
            return this;
        }
        public Builder noBorder() {
            borderWidth = 0; borderColor = null;
            explicit.add(Property.BORDER_WIDTH); explicit.add(Property.BORDER_COLOR);
            return this;
        }
        public Builder radius(int value) {
            if (value < 0) throw new IllegalArgumentException("radius cannot be negative");
            radius = value; explicit.add(Property.RADIUS); return this;
        }
        public Builder typography(TextStyle value) { typography = value; explicit.add(Property.TYPOGRAPHY); return this; }
        public Builder width(int value) { width = checked(value, "width"); explicit.add(Property.WIDTH); return this; }
        public Builder height(int value) { height = checked(value, "height"); explicit.add(Property.HEIGHT); return this; }
        public Builder minWidth(int value) { minWidth = checked(value, "minWidth"); explicit.add(Property.MIN_WIDTH); return this; }
        public Builder minHeight(int value) { minHeight = checked(value, "minHeight"); explicit.add(Property.MIN_HEIGHT); return this; }
        public Builder maxWidth(int value) { maxWidth = checked(value, "maxWidth"); explicit.add(Property.MAX_WIDTH); return this; }
        public Builder maxHeight(int value) { maxHeight = checked(value, "maxHeight"); explicit.add(Property.MAX_HEIGHT); return this; }
        public Builder autoWidth() { width = null; explicit.add(Property.WIDTH); return this; }
        public Builder autoHeight() { height = null; explicit.add(Property.HEIGHT); return this; }

        public Style build() {
            return new Style(margin, padding, background, borderColor, borderWidth, radius,
                    width, height, minWidth, minHeight, maxWidth, maxHeight, typography, explicit);
        }

        private static int checked(int value, String name) {
            if (value < 0) throw new IllegalArgumentException(name + " cannot be negative");
            return value;
        }
    }
}
