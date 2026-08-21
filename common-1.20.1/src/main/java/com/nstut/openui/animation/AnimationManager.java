package com.nstut.openui.animation;

import com.nstut.openui.api.UiRender;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class AnimationManager implements AutoCloseable {
    private final List<AnimationEntry> animations = new ArrayList<>();

    public Animation<Float> animate(float from, float to, long durationMillis, Easing easing, Consumer<Float> setter) {
        return animateFloat(from, to, durationMillis, easing, setter);
    }

    public Animation<Float> animateFloat(float from, float to, long durationMillis, Easing easing, Consumer<Float> setter) {
        FloatTween tween = new FloatTween(from, to, durationMillis, easing, setter);
        animations.add(tween);
        setter.accept(from);
        return tween;
    }

    public Animation<Integer> animateColor(int from, int to, long durationMillis, Easing easing, Consumer<Integer> setter) {
        ColorTween tween = new ColorTween(from, to, durationMillis, easing, setter);
        animations.add(tween);
        setter.accept(from);
        return tween;
    }

    public Animation<Void> animatePosition(int fromX, int fromY, int toX, int toY,
                                          long durationMillis, Easing easing,
                                          BiConsumer<Integer, Integer> setter) {
        PositionTween tween = new PositionTween(fromX, fromY, toX, toY, durationMillis, easing, setter);
        animations.add(tween);
        setter.accept(fromX, fromY);
        return tween;
    }

    public void tick(long nowNanos) {
        for (AnimationEntry animation : List.copyOf(animations)) {
            animation.tick(nowNanos);
            if (animation.isFinished()) animations.remove(animation);
        }
    }

    public int activeCount() { return animations.size(); }

    @Override public void close() {
        animations.forEach(AnimationEntry::cancel);
        animations.clear();
    }

    private interface AnimationEntry {
        void tick(long nowNanos);
        boolean isFinished();
        void cancel();
    }

    private static final class FloatTween implements Animation<Float>, AnimationEntry {
        private final float from, to;
        private final long durationNanos;
        private final Easing easing;
        private final Consumer<Float> setter;
        private long startNanos = -1;
        private float value;
        private boolean finished;

        private FloatTween(float from, float to, long durationMillis, Easing easing, Consumer<Float> setter) {
            this.from = from;
            this.to = to;
            this.durationNanos = Math.max(1, durationMillis) * 1_000_000L;
            this.easing = easing != null ? easing : Easing.LINEAR;
            this.setter = setter;
            this.value = from;
        }

        @Override
        public void tick(long nowNanos) {
            if (finished) return;
            if (startNanos < 0) startNanos = nowNanos;
            float progress = Math.min(1f, (float) (nowNanos - startNanos) / durationNanos);
            value = from + (to - from) * easing.apply(progress);
            setter.accept(value);
            if (progress >= 1f) finished = true;
        }

        @Override public Float value() { return value; }
        @Override public boolean isFinished() { return finished; }
        @Override public void cancel() { finished = true; }
    }

    private static final class ColorTween implements Animation<Integer>, AnimationEntry {
        private final int from, to;
        private final long durationNanos;
        private final Easing easing;
        private final Consumer<Integer> setter;
        private long startNanos = -1;
        private int value;
        private boolean finished;

        private ColorTween(int from, int to, long durationMillis, Easing easing, Consumer<Integer> setter) {
            this.from = from;
            this.to = to;
            this.durationNanos = Math.max(1, durationMillis) * 1_000_000L;
            this.easing = easing != null ? easing : Easing.LINEAR;
            this.setter = setter;
            this.value = from;
        }

        @Override
        public void tick(long nowNanos) {
            if (finished) return;
            if (startNanos < 0) startNanos = nowNanos;
            float progress = Math.min(1f, (float) (nowNanos - startNanos) / durationNanos);
            value = UiRender.mix(from, to, easing.apply(progress));
            setter.accept(value);
            if (progress >= 1f) finished = true;
        }

        @Override public Integer value() { return value; }
        @Override public boolean isFinished() { return finished; }
        @Override public void cancel() { finished = true; }
    }

    private static final class PositionTween implements Animation<Void>, AnimationEntry {
        private final int fromX, fromY, toX, toY;
        private final long durationNanos;
        private final Easing easing;
        private final BiConsumer<Integer, Integer> setter;
        private long startNanos = -1;
        private boolean finished;

        private PositionTween(int fromX, int fromY, int toX, int toY, long durationMillis, Easing easing,
                              BiConsumer<Integer, Integer> setter) {
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
            this.durationNanos = Math.max(1, durationMillis) * 1_000_000L;
            this.easing = easing != null ? easing : Easing.LINEAR;
            this.setter = setter;
        }

        @Override
        public void tick(long nowNanos) {
            if (finished) return;
            if (startNanos < 0) startNanos = nowNanos;
            float progress = Math.min(1f, (float) (nowNanos - startNanos) / durationNanos);
            float eased = easing.apply(progress);
            int curX = Math.round(fromX + (toX - fromX) * eased);
            int curY = Math.round(fromY + (toY - fromY) * eased);
            setter.accept(curX, curY);
            if (progress >= 1f) finished = true;
        }

        @Override public Void value() { return null; }
        @Override public boolean isFinished() { return finished; }
        @Override public void cancel() { finished = true; }
    }
}
