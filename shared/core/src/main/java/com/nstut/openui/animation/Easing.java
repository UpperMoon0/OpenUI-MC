package com.nstut.openui.animation;

@FunctionalInterface
public interface Easing {
    Easing LINEAR = value -> value;
    Easing EASE_IN = value -> value * value;
    Easing EASE_OUT = value -> 1 - (1 - value) * (1 - value);
    Easing EASE_IN_OUT = value -> value < 0.5f ? 2 * value * value : 1 - (float) Math.pow(-2 * value + 2, 2) / 2;
    Easing CUBIC_OUT = value -> 1 - (float) Math.pow(1 - value, 3);
    Easing BACK_OUT = value -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(value - 1, 3) + c1 * (float) Math.pow(value - 1, 2);
    };

    float apply(float progress);
}

