package com.nstut.openui.controls;

import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommandPaletteTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void typingFiltersCommandsAndEnterExecutesSelectedAction() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        AtomicBoolean action1Executed = new AtomicBoolean(false);
        AtomicBoolean action2Executed = new AtomicBoolean(false);

        CommandPalette palette = new CommandPalette()
                .item("cmd.save", "Save Project", () -> action1Executed.set(true))
                .item("cmd.export", "Export Data", () -> action2Executed.set(true));

        palette.layout(0, 0, 300, 200);

        // Type 'exp'
        palette.charTyped('e', 0);
        palette.charTyped('x', 0);
        palette.charTyped('p', 0);

        // Press Enter (key 257)
        palette.keyPressed(257, 0, 0);

        assertFalse(action1Executed.get(), "Save Project should be filtered out");
        assertTrue(action2Executed.get(), "Export Data should be selected and executed");
    }
}
