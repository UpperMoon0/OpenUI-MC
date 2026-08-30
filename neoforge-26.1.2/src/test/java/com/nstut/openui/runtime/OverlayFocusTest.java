package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
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
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        AtomicBoolean screenClicked = new AtomicBoolean(false);
        ButtonWidget screenBtn = Ui.button("Click Me", () -> screenClicked.set(true));

        runtime.setRoot(screenBtn);
        runtime.setViewport(0, 0, 300, 200);

        UIComponent modalCard = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 120; }
            @Override public int preferredHeight(Font font) { return 80; }
            @Override public void layout(int lx, int ly, int w, int h) { setBounds(50, 50, 120, 80); }
            @Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my, float pt) {}
        };
        runtime.overlays().show(OverlayLayer.MODAL, modalCard, true);

        // Click on the area where screenBtn is located, but outside modal card (10, 10)
        boolean handled = runtime.mouseClicked(10, 10, 0);
        assertTrue(handled, "Modal overlay should absorb and handle the click");
        assertFalse(screenClicked.get(), "Screen button should not have received click when modal is active");
    }

    @Test
    void nonBlockingOverlayDismissesOnOutsideClickAndPermitsUnderlyingClick() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        AtomicBoolean screenClicked = new AtomicBoolean(false);
        AtomicBoolean dropdownDismissed = new AtomicBoolean(false);

        ButtonWidget screenBtn = Ui.button("Click Me", () -> screenClicked.set(true));
        runtime.setRoot(screenBtn);
        runtime.setViewport(0, 0, 300, 200);

        UIComponent dropdown = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 80; }
            @Override public int preferredHeight(Font font) { return 60; }
            @Override public void layout(int lx, int ly, int w, int h) { setBounds(150, 50, 80, 60); }
            @Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my, float pt) {}
        };

        runtime.overlays().show(
                OverlayLayer.DROPDOWN,
                dropdown,
                false,
                true,
                true,
                () -> dropdownDismissed.set(true)
        );
        assertEquals(1, runtime.overlays().size());

        // Click outside dropdown directly on the screen button (15, 15)
        runtime.mouseClicked(15, 15, 0);

        assertTrue(dropdownDismissed.get(), "Dropdown should have been dismissed by outside click");
        assertTrue(screenClicked.get(), "Screen button click should have been dispatched");
        assertEquals(0, runtime.overlays().size());
    }

    private UIComponent keyCapture(AtomicBoolean keyPressed, AtomicBoolean charTyped) {
        return new UIComponent() {
            @Override public int preferredWidth(Font font) { return 100; }
            @Override public int preferredHeight(Font font) { return 60; }
            @Override public void layout(int lx, int ly, int w, int h) { setBounds(lx, ly, w, h); }
            @Override public void render(GuiGraphicsExtractor g, Font f, int mx, int my, float pt) {}
            @Override public boolean keyPressed(int key, int scanCode, int modifiers) { keyPressed.set(true); return true; }
            @Override public boolean charTyped(char character, int modifiers) { charTyped.set(true); return true; }
        };
    }

    @Test
    void unfocusedKeyEventsFallBackToRootForShortcuts() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        AtomicBoolean rootKey = new AtomicBoolean(false);
        AtomicBoolean rootChar = new AtomicBoolean(false);
        runtime.setRoot(keyCapture(rootKey, rootChar));

        assertTrue(runtime.keyPressed(82, 0, 0), "Unfocused keys should reach the root for screen shortcuts");
        assertTrue(rootKey.get());
    }

    @Test
    void charTypedDoesNotFallBackToRootWithoutFocus() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        AtomicBoolean rootKey = new AtomicBoolean(false);
        AtomicBoolean rootChar = new AtomicBoolean(false);
        runtime.setRoot(keyCapture(rootKey, rootChar));

        assertFalse(runtime.charTyped('a', 0));
        assertFalse(rootChar.get(), "Typed characters are text input and must not reach an unfocused root");
    }

    @Test
    void blockingOverlayScopesUnfocusedKeysToOverlay() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        AtomicBoolean rootKey = new AtomicBoolean(false);
        AtomicBoolean rootChar = new AtomicBoolean(false);
        runtime.setRoot(keyCapture(rootKey, rootChar));

        AtomicBoolean overlayKey = new AtomicBoolean(false);
        AtomicBoolean overlayChar = new AtomicBoolean(false);
        runtime.overlays().show(OverlayLayer.MODAL, keyCapture(overlayKey, overlayChar), true);

        assertTrue(runtime.keyPressed(82, 0, 0));
        assertTrue(overlayKey.get(), "Blocking overlay owns the keyboard scope");
        assertFalse(rootKey.get(), "Underlying root shortcuts must not fire behind a blocking overlay");
    }
}
