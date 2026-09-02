package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.Ui;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FocusManagerModernTest {
    @Test
    void legacyPushAndRestoreRoundTripsFocus() {
        ButtonWidget first = Ui.button("First", () -> { });
        ButtonWidget second = Ui.button("Second", () -> { });
        var root = Ui.column(first, second);
        FocusManager focus = new FocusManager();
        focus.setRoot(root);

        assertTrue(focus.requestFocus(first));
        focus.pushFocus();
        assertTrue(focus.requestFocus(second));

        focus.restoreFocus();
        assertSame(first, focus.focused());

        focus.restoreFocus();
        assertNull(focus.focused());
    }

    @Test
    void nestedTrapsRestoreTheirPreviousFocusInStackOrder() {
        ButtonWidget outside = Ui.button("Outside", () -> { });
        ButtonWidget outerButton = Ui.button("Outer", () -> { });
        ButtonWidget innerButton = Ui.button("Inner", () -> { });
        var inner = Ui.column(innerButton);
        var outer = Ui.column(outerButton, inner);
        var root = Ui.column(outside, outer);
        FocusManager focus = new FocusManager();
        focus.setRoot(root);

        assertTrue(focus.requestFocus(outside));
        focus.trapFocus(outer);
        assertSame(outerButton, focus.focused());
        assertSame(outer, focus.currentTrapRoot());

        focus.trapFocus(inner);
        assertSame(innerButton, focus.focused());
        assertSame(inner, focus.currentTrapRoot());

        focus.untrapFocus(inner);
        assertSame(outerButton, focus.focused());
        assertSame(outer, focus.currentTrapRoot());

        focus.untrapFocus(outer);
        assertSame(outside, focus.focused());
        assertFalse(focus.isTrapped());
    }

    @Test
    void activeTrapRejectsFocusRequestsOutsideItsSubtree() {
        ButtonWidget outside = Ui.button("Outside", () -> { });
        ButtonWidget inside = Ui.button("Inside", () -> { });
        var trap = Ui.column(inside);
        var root = Ui.column(outside, trap);
        FocusManager focus = new FocusManager();
        focus.setRoot(root);

        focus.trapFocus(trap);

        assertSame(inside, focus.focused());
        assertFalse(focus.requestFocus(outside));
        assertSame(inside, focus.focused());
    }
}
