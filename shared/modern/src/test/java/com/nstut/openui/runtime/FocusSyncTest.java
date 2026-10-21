package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.EditBoxWrapper;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.input.EventType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FocusSyncTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    @Disabled("Minecraft 26.1 EditBox focus requires a running client singleton")
    void focusManagerSynchronizesWithNativeEditBoxAndDispatchesFocusBlurEvents() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        EditBox nativeBox = new EditBox(font, 0, 0, 100, 20, Component.literal("Test"));
        EditBoxWrapper wrapper = new EditBoxWrapper(nativeBox);

        AtomicInteger focusCount = new AtomicInteger(0);
        AtomicInteger blurCount = new AtomicInteger(0);

        wrapper.on(EventType.FOCUS, e -> focusCount.incrementAndGet());
        wrapper.on(EventType.BLUR, e -> blurCount.incrementAndGet());

        ButtonWidget btn = Ui.button("Other", () -> {});

        UIComponent root = Ui.column(wrapper, btn);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);

        assertFalse(nativeBox.isFocused(), "Native edit box initially unfocused");
        assertFalse(wrapper.isFocused(), "Wrapper initially unfocused");

        // Focus wrapper via FocusManager
        runtime.focus().requestFocus(wrapper);

        assertTrue(wrapper.isFocused(), "Wrapper should be focused");
        assertTrue(nativeBox.isFocused(), "Native edit box MUST be focused when wrapper gains focus");
        assertEquals(1, focusCount.get(), "FOCUS event should be dispatched");
        assertEquals(0, blurCount.get(), "BLUR event should not be dispatched yet");

        // Move focus to button
        runtime.focus().requestFocus(btn);

        assertFalse(wrapper.isFocused(), "Wrapper should lose focus");
        assertFalse(nativeBox.isFocused(), "Native edit box MUST lose focus when wrapper loses focus");
        assertEquals(1, focusCount.get());
        assertEquals(1, blurCount.get(), "BLUR event should be dispatched on focus lost");
    }
}
