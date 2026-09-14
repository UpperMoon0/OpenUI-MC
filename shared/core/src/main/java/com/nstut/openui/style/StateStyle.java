package com.nstut.openui.style;

import java.util.Objects;

/** Immutable visual-state variants composed over one base style. */
public record StateStyle(Style base, Style hovered, Style focused, Style pressed, Style disabled) {
    public StateStyle {
        base = Objects.requireNonNullElse(base, Style.EMPTY);
        hovered = Objects.requireNonNullElse(hovered, Style.EMPTY);
        focused = Objects.requireNonNullElse(focused, Style.EMPTY);
        pressed = Objects.requireNonNullElse(pressed, Style.EMPTY);
        disabled = Objects.requireNonNullElse(disabled, Style.EMPTY);
    }

    public static StateStyle of(Style base) {
        return new StateStyle(base, Style.EMPTY, Style.EMPTY, Style.EMPTY, Style.EMPTY);
    }

    public Style resolve(boolean isHovered, boolean isFocused, boolean isPressed, boolean isDisabled) {
        Style result = base;
        if (isHovered) result = result.merge(hovered);
        if (isFocused) result = result.merge(focused);
        if (isPressed) result = result.merge(pressed);
        if (isDisabled) result = result.merge(disabled);
        return result;
    }
}
