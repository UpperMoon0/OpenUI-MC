package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.Ui;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FocusManagerTest {
    @Test
    void focusTraversalWrapsInTreeOrder() {
        ButtonWidget first = Ui.button("First", () -> { });
        ButtonWidget second = Ui.button("Second", () -> { });
        var root = Ui.column(first, second);
        FocusManager focus = new FocusManager();
        focus.setRoot(root);

        assertTrue(focus.focusNext());
        assertSame(first, focus.focused());
        assertTrue(focus.focusNext());
        assertSame(second, focus.focused());
        assertTrue(focus.focusNext());
        assertSame(first, focus.focused());
    }
}

