package com.nstut.openui.input;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Renderer-independent directional focus selection for keyboard/controllers. */
public final class SpatialNavigation {
    private SpatialNavigation() { }

    public static <T> Optional<T> next(Target<T> current, List<Target<T>> candidates, Direction direction) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(direction, "direction");
        double cx = current.centerX();
        double cy = current.centerY();

        return candidates.stream()
                .filter(candidate -> candidate != current && inDirection(cx, cy, candidate, direction))
                .min(Comparator.comparingDouble(candidate -> score(cx, cy, candidate, direction)))
                .map(Target::value);
    }

    private static boolean inDirection(double x, double y, Target<?> target, Direction direction) {
        return switch (direction) {
            case LEFT -> target.centerX() < x;
            case RIGHT -> target.centerX() > x;
            case UP -> target.centerY() < y;
            case DOWN -> target.centerY() > y;
        };
    }

    private static double score(double x, double y, Target<?> target, Direction direction) {
        double dx = Math.abs(target.centerX() - x);
        double dy = Math.abs(target.centerY() - y);
        double primary = direction == Direction.LEFT || direction == Direction.RIGHT ? dx : dy;
        double secondary = direction == Direction.LEFT || direction == Direction.RIGHT ? dy : dx;
        return primary * 1000.0 + secondary;
    }

    public enum Direction { LEFT, RIGHT, UP, DOWN }

    public record Target<T>(T value, int x, int y, int width, int height) {
        public Target {
            Objects.requireNonNull(value, "value");
            if (width < 0 || height < 0) throw new IllegalArgumentException("Target size cannot be negative");
        }
        double centerX() { return x + width / 2.0; }
        double centerY() { return y + height / 2.0; }
    }
}
