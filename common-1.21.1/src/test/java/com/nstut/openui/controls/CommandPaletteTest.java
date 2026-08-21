package com.nstut.openui.controls;

import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

        palette.charTyped('e', 0);
        palette.charTyped('x', 0);
        palette.charTyped('p', 0);

        palette.keyPressed(257, 0, 0);

        assertFalse(action1Executed.get(), "Save Project should be filtered out");
        assertTrue(action2Executed.get(), "Export Data should be selected and executed");
    }

    @Test
    void clickingBelowListDoesNotExecuteInvisibleRow() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        AtomicInteger executedCount = new AtomicInteger(0);

        CommandPalette palette = new CommandPalette()
                .item("cmd.one", "One", () -> executedCount.incrementAndGet())
                .item("cmd.two", "Two", () -> executedCount.incrementAndGet())
                .item("cmd.three", "Three", () -> executedCount.incrementAndGet());

        palette.layout(0, 0, 300, 200);

        int listY = palette.getY() + 36;
        int listH = palette.getHeight() - 42;
        int maxVisible = Math.max(1, listH / 20);
        int visiblePixelHeight = maxVisible * 20;

        int clickBelowList = listY + visiblePixelHeight + 5;
        palette.mouseClicked(palette.getX() + 10, clickBelowList, 0);

        assertEquals(0, executedCount.get(), "Clicking below visible list area should not execute any item");
    }

    @Test
    void mouseWheelScrollsCommandPaletteList() throws Exception {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        CommandPalette palette = new CommandPalette()
                .item("cmd.a", "A", () -> {})
                .item("cmd.b", "B", () -> {})
                .item("cmd.c", "C", () -> {})
                .item("cmd.d", "D", () -> {})
                .item("cmd.e", "E", () -> {})
                .item("cmd.f", "F", () -> {})
                .item("cmd.g", "G", () -> {});

        palette.layout(0, 0, 300, 200);

        Field scrollOffsetField = CommandPalette.class.getDeclaredField("scrollOffset");
        scrollOffsetField.setAccessible(true);
        int initialScroll = (int) scrollOffsetField.get(palette);

        palette.mouseScrolled(palette.getX() + 10, palette.getY() + 50, -1);

        int afterScroll = (int) scrollOffsetField.get(palette);
        assertEquals(initialScroll + 1, afterScroll, "Scroll wheel down should increase scrollOffset");
    }
}
