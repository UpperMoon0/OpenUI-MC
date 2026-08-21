package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.Checkbox;
import com.nstut.openui.controls.Select;
import com.nstut.openui.controls.SwitchControl;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class InteractionTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void simulatedButtonClickDispatchesAction() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
        AtomicBoolean clicked = new AtomicBoolean(false);

        ButtonWidget btn = Ui.button("Test", () -> clicked.set(true));
        runtime.setRoot(btn);
        runtime.setViewport(0, 0, 200, 100);

        // Click on button
        runtime.mouseClicked(10, 10, 0);
        runtime.mouseReleased(10, 10, 0);

        assertTrue(clicked.get());
    }

    @Test
    void checkboxTogglesStateOnSimulatedClick() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
        Signal<Boolean> checked = Signals.of(false);

        Checkbox cb = Ui.checkbox("Check", checked);
        runtime.setRoot(cb);
        runtime.setViewport(0, 0, 200, 100);

        assertFalse(checked.get());

        runtime.mouseClicked(5, 5, 0);
        assertTrue(checked.get());

        runtime.mouseClicked(5, 5, 0);
        assertFalse(checked.get());
    }

    @Test
    void selectDropdownKeyboardArrowNavigation() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
        Signal<String> selection = Signals.of("A");

        Select<String> select = Ui.select(selection)
                .option("A", "A")
                .option("B", "B")
                .option("C", "C");

        runtime.setRoot(select);
        runtime.focus().requestFocus(select);

        assertEquals("A", selection.get());

        // Press Down arrow (key 264)
        runtime.keyPressed(264, 0, 0);
        assertEquals("B", selection.get());

        // Press Down arrow again
        runtime.keyPressed(264, 0, 0);
        assertEquals("C", selection.get());

        // Press Up arrow (key 265)
        runtime.keyPressed(265, 0, 0);
        assertEquals("B", selection.get());
    }
}
