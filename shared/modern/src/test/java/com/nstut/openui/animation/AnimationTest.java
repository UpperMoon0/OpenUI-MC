package com.nstut.openui.animation;

import com.nstut.openui.theme.Theme;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AnimationTest {

    @Test
    void animationInterpolatesValuesOverTime() {
        AnimationManager manager = new AnimationManager();
        AtomicReference<Float> current = new AtomicReference<>(0.0F);

        Animation<Float> anim = manager.animateFloat(0.0F, 100.0F, 200, Easing.LINEAR, current::set);
        assertEquals(1, manager.activeCount());
        assertEquals(0.0F, current.get(), 0.001F);

        long startNanos = 1_000_000_000L;
        manager.tick(startNanos);
        assertEquals(0.0F, current.get(), 0.001F);

        // 100ms later (50% progress)
        manager.tick(startNanos + 100_000_000L);
        assertEquals(50.0F, current.get(), 0.1F);

        // 200ms later (100% progress)
        manager.tick(startNanos + 200_000_000L);
        assertEquals(100.0F, current.get(), 0.001F);
        assertTrue(anim.isFinished());
        assertEquals(0, manager.activeCount());
    }

    @Test
    void animationCancellationStopsUpdates() {
        AnimationManager manager = new AnimationManager();
        AtomicReference<Float> current = new AtomicReference<>(0.0F);

        Animation<Float> anim = manager.animateFloat(0.0F, 100.0F, 200, Easing.LINEAR, current::set);
        long start = 1_000_000_000L;
        manager.tick(start);
        manager.tick(start + 50_000_000L);
        assertEquals(25.0F, current.get(), 0.1F);

        anim.cancel();
        assertTrue(anim.isFinished());

        manager.tick(start + 150_000_000L);
        assertEquals(25.0F, current.get(), 0.1F); // No further progress
        assertEquals(0, manager.activeCount());
    }

    @Test
    void reducedMotionDisablesDurationsInTheme() {
        Theme normal = Theme.dark();
        assertFalse(normal.reducedMotion());
        assertEquals(120, normal.durations().hoverMs());

        Theme reduced = normal.withReducedMotion(true);
        assertTrue(reduced.reducedMotion());
    }
}
