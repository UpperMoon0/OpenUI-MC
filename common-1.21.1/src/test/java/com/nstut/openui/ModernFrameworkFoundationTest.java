package com.nstut.openui;

import com.nstut.openui.declarative.DeclarativeChild;
import com.nstut.openui.input.SpatialNavigation;
import com.nstut.openui.runtime.FrameScheduler;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ModernFrameworkFoundationTest {
    @Test
    void schedulerCoalescesKeyedWorkAndPreservesOrder() {
        FrameScheduler scheduler = new FrameScheduler();
        List<String> calls = new ArrayList<>();
        scheduler.schedule("same", () -> calls.add("first"));
        scheduler.schedule("same", () -> calls.add("ignored"));
        scheduler.schedule(() -> calls.add("immediate"));
        scheduler.schedule("other", () -> calls.add("other"));
        scheduler.flush();
        assertEquals(List.of("immediate", "first", "other"), calls);
    }

    @Test
    void schedulerDetectsRunawayLoops() {
        FrameScheduler scheduler = new FrameScheduler();
        scheduler.maxPasses(3);
        Runnable[] loop = new Runnable[1];
        loop[0] = () -> scheduler.schedule(loop[0]);
        scheduler.schedule(loop[0]);
        assertThrows(IllegalStateException.class, scheduler::flush);
    }

    @Test
    void declarativeDescriptionUpdatesCreatedAndReusedInstances() {
        AtomicInteger updates = new AtomicInteger();
        DeclarativeChild<StringBuilder> child = new DeclarativeChild<>(
                "text", "status", StringBuilder::new, value -> {
                    value.append('x');
                    updates.incrementAndGet();
                });
        StringBuilder created = child.create();
        child.apply(created);
        assertEquals("xx", created.toString());
        assertEquals(2, updates.get());
    }

    @Test
    void stateStyleComposesVariants() {
        Style base = Style.builder().padding(2).background(0xFF101010).build();
        Style hover = Style.builder().background(0xFF202020).build();
        StateStyle states = new StateStyle(base, hover, Style.EMPTY, Style.EMPTY, Style.EMPTY);
        Style resolved = states.resolve(true, false, false, false);
        assertEquals(0xFF202020, resolved.background());
        assertEquals(Style.Insets.all(2), resolved.padding());
    }

    @Test
    void spatialNavigationPrefersNearestTargetInRequestedDirection() {
        SpatialNavigation.Target<String> current = new SpatialNavigation.Target<>("center", 10, 10, 10, 10);
        List<SpatialNavigation.Target<String>> candidates = List.of(
                new SpatialNavigation.Target<>("far", 100, 10, 10, 10),
                new SpatialNavigation.Target<>("near", 30, 12, 10, 10),
                new SpatialNavigation.Target<>("left", 0, 10, 5, 5));
        assertEquals("near", SpatialNavigation.next(current, candidates, SpatialNavigation.Direction.RIGHT).orElseThrow());
    }
}
