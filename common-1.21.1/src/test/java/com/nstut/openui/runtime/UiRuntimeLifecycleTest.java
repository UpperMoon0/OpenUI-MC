package com.nstut.openui.runtime;

import com.nstut.openui.api.DeclarativeHost;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.declarative.DeclarativeChild;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiRuntimeLifecycleTest {
    @Test
    void closeClearsQueuedDeclarativeFrameWork() {
        AtomicInteger builds = new AtomicInteger();
        DeclarativeHost host = new DeclarativeHost(scope -> {
            builds.incrementAndGet();
            return List.of(new DeclarativeChild<>(
                    "fixed", "child", () -> new FixedComponent(), ignored -> { }));
        });
        Font font = new Font(null, false);
        NativeWidgetHost widgets = new NativeWidgetHost() {
            @Override public void add(AbstractWidget widget) { }
            @Override public void remove(AbstractWidget widget) { }
        };
        UiRuntime runtime = new UiRuntime(font, widgets);
        runtime.setRoot(host);

        assertTrue(runtime.hasPendingFrameTasks(),
                "mounting a declarative root should queue its initial frame build");
        assertEquals(0, builds.get(), "the queued initial build must not run before a frame flush");

        runtime.close();

        assertFalse(runtime.hasPendingFrameTasks(),
                "closing the runtime must release every queued callback and its captured runtime graph");
        runtime.scheduleFrame("after-close", builds::incrementAndGet);
        runtime.flushFrameTasks();
        assertFalse(runtime.hasPendingFrameTasks(), "closed runtimes must reject newly scheduled frame work");
        assertEquals(0, builds.get(), "closed runtime work must never execute after teardown");
    }

    private static final class FixedComponent extends UIComponent {
        @Override public int preferredWidth(Font font) { return 1; }
        @Override public int preferredHeight(Font font) { return 1; }
        @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) { }
    }
}
