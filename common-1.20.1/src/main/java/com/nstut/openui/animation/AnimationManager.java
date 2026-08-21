package com.nstut.openui.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AnimationManager implements AutoCloseable {
    private final List<FloatTween> animations = new ArrayList<>();

    public Animation<Float> animateFloat(float from, float to, long durationMillis, Easing easing, Consumer<Float> setter) {
        FloatTween tween = new FloatTween(from, to, durationMillis, easing, setter);
        animations.add(tween);
        setter.accept(from);
        return tween;
    }

    public void tick(long nowNanos) {
        for (FloatTween animation : List.copyOf(animations)) {
            animation.tick(nowNanos);
            if (animation.isFinished()) animations.remove(animation);
        }
    }

    public int activeCount() { return animations.size(); }

    @Override public void close() {
        animations.forEach(FloatTween::cancel);
        animations.clear();
    }

    private static final class FloatTween implements Animation<Float> {
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
            this.easing = easing;
            this.setter = setter;
            this.value = from;
        }

        private void tick(long nowNanos) {
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
}
