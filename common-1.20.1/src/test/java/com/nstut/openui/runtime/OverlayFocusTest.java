package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.Dialog;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.overlay.OverlayManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class OverlayFocusTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void modalTrapsFocusWithinModalSubtree() {
        ButtonWidget screenBtn1 = Ui.button("Screen 1", () -> {});
        ButtonWidget screenBtn2 = Ui.button("Screen 2", () -> {});
        UIComponent screenRoot = Ui.column(screenBtn1, screenBtn2);

        FocusManager focus = new FocusManager();
        focus.setRoot(screenRoot);

        focus.focusNext();
        assertSame(screenBtn1, focus.focused());

        ButtonWidget modalBtn1 = Ui.button("Modal OK", () -> {});
        ButtonWidget modalBtn2 = Ui.button("Modal Cancel", () -> {});
        UIComponent modalRoot = Ui.column(modalBtn1, modalBtn2);

        focus.trapFocus(modalRoot);
        assertSame(modalBtn1, focus.focused());

        assertTrue(focus.focusNext());
        assertSame(modalBtn2, focus.focused());

        assertTrue(focus.focusNext());
        assertSame(modalBtn1, focus.focused()); // Trapped within modal!

        focus.untrapFocus(modalRoot);
        assertSame(screenBtn1, focus.focused()); // Restored previous focus!
    }

    @Test
    void overlayEscapeDismissalFollowsPriorityOrder() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
        runtime.setRoot(Ui.text("Root"));

        AtomicBoolean dropdownClosed = new AtomicBoolean(false);
        AtomicBoolean modalClosed = new AtomicBoolean(false);

        OverlayHandle modalHandle = runtime.overlays().show(
                OverlayLayer.MODAL,
                Ui.text("Modal Content"),
                true,
                true,
                false,
                () -> modalClosed.set(true)
        );

        OverlayHandle dropdownHandle = runtime.overlays().show(
                OverlayLayer.DROPDOWN,
                Ui.text("Dropdown Content"),
                false,
                true,
                true,
                () -> dropdownClosed.set(true)
        );

        assertEquals(2, runtime.overlays().size());

        // Press Escape (key 256): closes topmost dismissable overlay (the dropdown first)
        assertTrue(runtime.keyPressed(256, 0, 0));
        assertTrue(dropdownClosed.get());
        assertFalse(modalClosed.get());
        assertEquals(1, runtime.overlays().size());

        // Press Escape again: closes modal
        assertTrue(runtime.keyPressed(256, 0, 0));
        assertTrue(modalClosed.get());
        assertEquals(0, runtime.overlays().size());
    }

    @Test
    void blockingOverlayBlocksClicksToUnderlyingRoot() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
        AtomicBoolean screenClicked = new AtomicBoolean(false);
        ButtonWidget screenBtn = Ui.button("Click Me", () -> screenClicked.set(true));
        screenBtn.layout(10, 10, 100, 20);
        runtime.setRoot(screenBtn);

        UIComponent modalCard = Ui.card(Ui.text("Modal")).width(120).height(80);
        modalCard.layout(0, 0, 120, 80);
        runtime.overlays().show(OverlayLayer.MODAL, modalCard, true);

        // Click outside modal card
        boolean handled = runtime.mouseClicked(150, 150, 0);
        assertFalse(screenClicked.get());
    }
}
