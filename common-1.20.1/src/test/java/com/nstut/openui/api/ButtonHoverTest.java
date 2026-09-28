package com.nstut.openui.api;

import com.nstut.openui.controls.Card;
import com.nstut.openui.input.EventType;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ButtonHoverTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void ancestorClipSuppressesDescendantHoverStateAndEvents() {
        Font font=new Font(null, false);
        UiRuntime runtime=new UiRuntime(font,dummyHost);
        try {
            AtomicInteger enter=new AtomicInteger();
            AtomicInteger leave=new AtomicInteger();
            ButtonWidget button=Ui.button("Clipped",()->{});
            button.on(EventType.HOVER_ENTER,e->enter.incrementAndGet());
            button.on(EventType.HOVER_LEAVE,e->leave.incrementAndGet());
            Card card=new Card(button).padding(0).radius(0).elevated(false);
            runtime.setRoot(card);
            runtime.setViewport(0,0,100,30);

            // Force real runtime layout, then place the child completely below the Card's protected region.
            assertFalse(runtime.mouseClicked(-10,-10,1));
            button.layout(5,40,40,10);
            runtime.preRender(10,45);
            assertFalse(button.isHovered(),
                    "a pointer over an invisible overflow rectangle must not hover the descendant");
            assertEquals(0,enter.get());

            // Hover works while visible, then is cleared when the same control becomes clipped again.
            button.layout(5,5,40,10);
            runtime.preRender(10,10);
            assertTrue(button.isHovered());
            assertEquals(1,enter.get());

            button.layout(5,40,40,10);
            runtime.preRender(10,45);
            assertFalse(button.isHovered());
            assertEquals(1,leave.get(),
                    "leaving an ancestor clip must emit hover-leave and clear state");
        } finally {
            runtime.close();
        }
    }

    @Test
    void hoverTransitionsDispatchEventsAndAnimateHoverProgress() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        AtomicInteger hoverEnterCount = new AtomicInteger(0);
        AtomicInteger hoverLeaveCount = new AtomicInteger(0);

        ButtonWidget btn = Ui.button("Test", () -> {});
        btn.on(EventType.HOVER_ENTER, e -> hoverEnterCount.incrementAndGet());
        btn.on(EventType.HOVER_LEAVE, e -> hoverLeaveCount.incrementAndGet());

        runtime.setRoot(btn);
        runtime.setViewport(0, 0, 300, 200);
        btn.layout(10, 10, 80, 20);

        assertEquals(0.0F, btn.getHoverProgress(), 0.001F);

        // Hover over button
        runtime.preRender(20, 20);

        assertTrue(btn.isHovered(), "Button should be hovered");
        assertEquals(1, hoverEnterCount.get(), "HOVER_ENTER should be dispatched");
        assertEquals(0, hoverLeaveCount.get());

        // Step animation: initialize start time then step 100ms
        runtime.animations().update(1_000_000_000L);
        runtime.animations().update(1_100_000_000L);
        assertTrue(btn.getHoverProgress() > 0.0F, "Hover progress should animate above 0");

        // Hover away
        runtime.preRender(200, 150);

        assertFalse(btn.isHovered(), "Button should no longer be hovered");
        assertEquals(1, hoverEnterCount.get());
        assertEquals(1, hoverLeaveCount.get(), "HOVER_LEAVE should be dispatched");

        // Step animation to completion (300ms)
        runtime.animations().update(1_200_000_000L);
        runtime.animations().update(1_500_000_000L);
        assertEquals(0.0F, btn.getHoverProgress(), 0.001F, "Hover progress should return to 0");
    }
}
