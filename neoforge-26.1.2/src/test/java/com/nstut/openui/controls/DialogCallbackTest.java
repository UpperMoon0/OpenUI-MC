package com.nstut.openui.controls;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DialogCallbackTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void showWrapsRawContentInStandardDialogShell() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        Dialog.show(runtime.overlays(), com.nstut.openui.api.Ui.text("Body"));

        Dialog.DialogContainer container = assertInstanceOf(
                Dialog.DialogContainer.class, runtime.overlays().components().get(0));
        assertInstanceOf(Card.class, container.getContent());
    }

    @Test
    void confirmButtonClickFiresConfirmOnlyOnceAndNotCancel() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        AtomicInteger confirmCount = new AtomicInteger(0);
        AtomicInteger cancelCount = new AtomicInteger(0);

        OverlayHandle handle = Dialog.confirm(
                runtime.overlays(),
                "Title",
                "Message",
                confirmCount::incrementAndGet,
                cancelCount::incrementAndGet
        );

        assertEquals(1, runtime.overlays().size());

        // Perform layout pass so dialog and button bounds are calculated
        runtime.overlays().layout(font, 0, 0, 300, 200);

        // Find buttons inside the dialog (0: Cancel, 1: Confirm)
        UIComponent modalRoot = runtime.overlays().components().get(0);
        List<ButtonWidget> buttons = findAllButtons(modalRoot);
        assertEquals(2, buttons.size());
        ButtonWidget confirmBtn = buttons.get(1);

        // Click confirm button
        confirmBtn.mouseClicked(confirmBtn.getX() + 2, confirmBtn.getY() + 2, 0);

        assertEquals(1, confirmCount.get(), "Confirm callback should execute exactly once");
        assertEquals(0, cancelCount.get(), "Cancel callback must NOT execute when confirmed");
        assertEquals(0, runtime.overlays().size(), "Dialog overlay should be closed");
    }

    @Test
    void cancelButtonClickFiresCancelOnlyOnce() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        AtomicInteger confirmCount = new AtomicInteger(0);
        AtomicInteger cancelCount = new AtomicInteger(0);

        OverlayHandle handle = Dialog.confirm(
                runtime.overlays(),
                "Title",
                "Message",
                confirmCount::incrementAndGet,
                cancelCount::incrementAndGet
        );

        // Perform layout pass so dialog and button bounds are calculated
        runtime.overlays().layout(font, 0, 0, 300, 200);

        UIComponent modalRoot = runtime.overlays().components().get(0);
        List<ButtonWidget> buttons = findAllButtons(modalRoot);
        assertEquals(2, buttons.size());
        ButtonWidget cancelBtn = buttons.get(0);

        // Click cancel button
        cancelBtn.mouseClicked(cancelBtn.getX() + 2, cancelBtn.getY() + 2, 0);

        assertEquals(0, confirmCount.get(), "Confirm callback must NOT execute when cancelled");
        assertEquals(1, cancelCount.get(), "Cancel callback should execute exactly once");
        assertEquals(0, runtime.overlays().size());
    }

    @Test
    void backdropClickDismissesDialogAndExecutesCancel() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        runtime.setViewport(0, 0, 300, 200);
        runtime.setRoot(com.nstut.openui.api.Ui.text("Screen"));

        AtomicInteger confirmCount = new AtomicInteger(0);
        AtomicInteger cancelCount = new AtomicInteger(0);

        Dialog.confirm(
                runtime.overlays(),
                "Title",
                "Message",
                confirmCount::incrementAndGet,
                cancelCount::incrementAndGet
        );

        // Layout dialog
        runtime.overlays().layout(font, 0, 0, 300, 200);
        assertEquals(1, runtime.overlays().size());

        // Click at top-left corner (10, 10), which is outside the centered dialog card
        boolean handled = runtime.mouseClicked(10, 10, 0);

        assertTrue(handled, "Modal overlay should absorb backdrop click");
        assertEquals(0, confirmCount.get(), "Confirm should not fire");
        assertEquals(1, cancelCount.get(), "Cancel should fire on backdrop dismissal");
        assertEquals(0, runtime.overlays().size(), "Dialog should be dismissed");
    }

    @Test
    void escapeDismissalFiresCancelCallbackOnce() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        runtime.setViewport(0, 0, 300, 200);

        AtomicInteger confirmCount = new AtomicInteger(0);
        AtomicInteger cancelCount = new AtomicInteger(0);

        Dialog.confirm(
                runtime.overlays(),
                "Title",
                "Message",
                confirmCount::incrementAndGet,
                cancelCount::incrementAndGet
        );

        // Press Escape (key 256)
        runtime.keyPressed(256, 0, 0);

        assertEquals(0, confirmCount.get());
        assertEquals(1, cancelCount.get());
        assertEquals(0, runtime.overlays().size());
    }

    private List<ButtonWidget> findAllButtons(UIComponent root) {
        List<ButtonWidget> list = new ArrayList<>();
        collectButtons(root, list);
        return list;
    }

    private void collectButtons(UIComponent root, List<ButtonWidget> list) {
        if (root instanceof ButtonWidget btn) list.add(btn);
        for (UIComponent child : root.children()) collectButtons(child, list);
    }
}
