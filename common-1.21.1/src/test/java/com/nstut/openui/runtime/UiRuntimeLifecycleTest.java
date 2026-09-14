package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.DeclarativeHost;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.declarative.DeclarativeChild;
import com.nstut.openui.declarative.DeclarativeTree;
import com.nstut.openui.input.SpatialNavigation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UiRuntimeLifecycleTest {
    @Test
    void closeClearsQueuedDeclarativeFrameWork() {
        AtomicInteger builds = new AtomicInteger();
        DeclarativeHost host = new DeclarativeHost(scope -> {
            builds.incrementAndGet();
            return List.of(new DeclarativeChild<>(
                    "fixed", "child", () -> new FixedComponent(), ignored -> { }));
        });
        UiRuntime runtime = runtime();
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

    @Test
    void removingFocusedChildClearsFocusBeforeDetachAndStopsKeyboardDispatch() {
        TrackingContainer root = new TrackingContainer();
        TrackingComponent child = new TrackingComponent();
        root.addChild(child);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            assertTrue(runtime.focus().requestFocus(child));
            assertTrue(root.isFocusWithin());

            assertTrue(root.removeChild(child));

            assertNull(runtime.focus().focused());
            assertEquals(1, child.focusLost);
            assertEquals(1, root.focusWithinLost);
            assertFalse(root.isFocusWithin());
            assertFalse(runtime.keyPressed(257, 0, 0));
            assertFalse(runtime.charTyped('x', 0));
            assertEquals(0, child.keyPresses);
            assertEquals(0, child.typedCharacters);
        } finally {
            runtime.close();
        }
    }

    @Test
    void keyedDeclarativeReplacementAndRemovalCannotRetainDetachedFocus() {
        TrackingContainer root = new TrackingContainer();
        UiRuntime runtime = runtime();
        DeclarativeTree<UIComponent> tree = new DeclarativeTree<>(new DeclarativeTree.Adapter<>() {
            @Override public void attach(UIComponent parent, UIComponent child) { parent.addChild(child); }
            @Override public void detach(UIComponent parent, UIComponent child) { parent.removeChild(child); }
            @Override public List<UIComponent> children(UIComponent parent) { return parent.children(); }
            @Override public void reorder(UIComponent parent, List<UIComponent> children) {
                ((TrackingContainer) parent).reorderChildren(children);
            }
        });
        TrackingComponent first = new TrackingComponent();
        TrackingComponent replacement = new TrackingComponent();
        try {
            runtime.setRoot(root);
            tree.reconcile(root, List.of(new DeclarativeChild<>(
                    "first", "stable", () -> first, ignored -> { })));
            assertSame(first, root.child(0));
            assertTrue(runtime.focus().requestFocus(first));

            tree.reconcile(root, List.of(new DeclarativeChild<>(
                    "replacement", "stable", () -> replacement, ignored -> { })));

            assertSame(replacement, root.child(0));
            assertNull(runtime.focus().focused());
            assertEquals(1, first.focusLost);
            assertFalse(runtime.keyPressed(257, 0, 0));
            assertEquals(0, first.keyPresses);

            assertTrue(runtime.focus().requestFocus(replacement));
            tree.reconcile(root, List.of());

            assertEquals(0, root.childCount());
            assertNull(runtime.focus().focused());
            assertEquals(1, replacement.focusLost);
            assertFalse(runtime.keyPressed(32, 0, 0));
            assertEquals(0, replacement.keyPresses);
        } finally {
            runtime.close();
        }
    }

    @Test
    void keyboardAndTextDispatchDefensivelyRejectStaleFocus() {
        TrackingContainer root = new TrackingContainer();
        TrackingComponent child = new TrackingComponent();
        root.addChild(child);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            assertTrue(runtime.focus().requestFocus(child));
            child.severParentForTest();

            assertFalse(runtime.keyPressed(257, 0, 0));
            assertFalse(runtime.charTyped('x', 0));
            assertNull(runtime.focus().focused());
            assertEquals(1, child.focusLost);
            assertEquals(0, child.keyPresses);
            assertEquals(0, child.typedCharacters);
        } finally {
            runtime.close();
        }
    }

    @Test
    void hidingFocusedButtonClearsFocusBeforeKeyboardActivation() {
        TrackingContainer root = new TrackingContainer();
        AtomicInteger clicks = new AtomicInteger();
        ButtonWidget button = new ButtonWidget("Hidden").onPress(clicks::incrementAndGet);
        root.addChild(button);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            assertTrue(runtime.focus().requestFocus(button));
            assertTrue(root.isFocusWithin());

            button.setVisible(false);

            assertFalse(runtime.keyPressed(257, 0, 0));
            assertFalse(runtime.keyPressed(32, 0, 0));
            assertNull(runtime.focus().focused());
            assertEquals(1, root.focusWithinLost);
            assertEquals(0, clicks.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void hidingFocusedAncestorBlocksKeyboardAndTextDispatch() {
        TrackingContainer root = new TrackingContainer();
        TrackingContainer parent = new TrackingContainer();
        TrackingComponent child = new TrackingComponent();
        parent.addChild(child);
        root.addChild(parent);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            assertTrue(runtime.focus().requestFocus(child));
            assertTrue(parent.isFocusWithin());
            assertTrue(root.isFocusWithin());

            parent.setVisible(false);

            assertFalse(runtime.keyPressed(257, 0, 0));
            assertFalse(runtime.charTyped('x', 0));
            assertNull(runtime.focus().focused());
            assertEquals(1, child.focusLost);
            assertEquals(1, parent.focusWithinLost);
            assertEquals(1, root.focusWithinLost);
            assertEquals(0, child.keyPresses);
            assertEquals(0, child.typedCharacters);
        } finally {
            runtime.close();
        }
    }

    @Test
    void buttonDefensivelyRejectsDirectKeyboardActivationThroughHiddenAncestor() {
        TrackingContainer root = new TrackingContainer();
        TrackingContainer parent = new TrackingContainer();
        AtomicInteger clicks = new AtomicInteger();
        ButtonWidget button = new ButtonWidget("Hidden ancestor").onPress(clicks::incrementAndGet);
        parent.addChild(button);
        root.addChild(parent);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            parent.setVisible(false);

            assertFalse(button.keyPressed(257, 0, 0));
            assertFalse(button.keyPressed(32, 0, 0));
            assertEquals(0, clicks.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void focusedComponentBecomingNonFocusableCannotReceiveKeyboardOrText() {
        TrackingContainer root = new TrackingContainer();
        TrackingComponent child = new TrackingComponent();
        root.addChild(child);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            assertTrue(runtime.focus().requestFocus(child));

            child.focusable(false);

            assertFalse(runtime.keyPressed(257, 0, 0));
            assertFalse(runtime.charTyped('x', 0));
            assertNull(runtime.focus().focused());
            assertEquals(1, child.focusLost);
            assertEquals(0, child.keyPresses);
            assertEquals(0, child.typedCharacters);
        } finally {
            runtime.close();
        }
    }

    @Test
    void disablingFocusedButtonClearsFocusAndRestoresRootKeyboardRouting() {
        RootKeyCapture root = new RootKeyCapture();
        AtomicInteger clicks = new AtomicInteger();
        ButtonWidget button = new ButtonWidget("Disable me").onPress(clicks::incrementAndGet);
        root.addChild(button);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(root);
            assertTrue(runtime.focus().requestFocus(button));

            button.enabled(false);

            assertFalse(button.isFocusable());
            assertNull(runtime.focus().focused());
            assertTrue(runtime.keyPressed(82, 0, 0), "unfocused keyboard routing should return to the root");
            assertEquals(1, root.keyPresses);
            assertEquals(0, clicks.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void tabAndSpatialNavigationSkipDisabledButtons() {
        ButtonWidget first = new ButtonWidget("First");
        ButtonWidget disabled = new ButtonWidget("Disabled").enabled(false);
        ButtonWidget last = new ButtonWidget("Last");
        UIComponent row = Ui.row(first, disabled, last);
        row.layoutTree(new Font(null, false), 0, 0, 300, 40);

        FocusManager focus = new FocusManager();
        focus.setRoot(row);

        assertTrue(focus.focusNext());
        assertSame(first, focus.focused());
        assertTrue(focus.focusNext());
        assertSame(last, focus.focused(), "Tab navigation must skip disabled buttons");
        assertFalse(focus.requestFocus(disabled), "disabled buttons must reject direct focus requests");

        assertTrue(focus.requestFocus(first));
        assertTrue(focus.focusDirection(SpatialNavigation.Direction.RIGHT));
        assertSame(last, focus.focused(), "spatial navigation must skip disabled buttons");
    }

    @Test
    void modalFocusTrapSkipsDisabledButtons() {
        ButtonWidget disabled = new ButtonWidget("Disabled").enabled(false);
        ButtonWidget enabled = new ButtonWidget("Enabled");
        UIComponent modal = Ui.column(disabled, enabled);
        FocusManager focus = new FocusManager();

        focus.trapFocus(modal);

        assertSame(enabled, focus.focused(), "focus traps must select the first enabled focusable control");
        focus.untrapFocus(modal);
    }

    @Test
    void clickingDisabledButtonDoesNotFocusOrActivateIt() {
        AtomicInteger clicks = new AtomicInteger();
        ButtonWidget button = new ButtonWidget("Disabled").onPress(clicks::incrementAndGet).enabled(false);
        UiRuntime runtime = runtime();
        try {
            runtime.setRoot(button);
            runtime.setViewport(0, 0, 120, 30);

            assertFalse(runtime.mouseClicked(10, 10, 0));
            assertNull(runtime.focus().focused());
            assertEquals(0, clicks.get());
        } finally {
            runtime.close();
        }
    }

    private static UiRuntime runtime() {
        Font font = new Font(null, false);
        NativeWidgetHost widgets = new NativeWidgetHost() {
            @Override public void add(AbstractWidget widget) { }
            @Override public void remove(AbstractWidget widget) { }
        };
        return new UiRuntime(font, widgets);
    }

    private static class FixedComponent extends UIComponent {
        @Override public int preferredWidth(Font font) { return 1; }
        @Override public int preferredHeight(Font font) { return 1; }
        @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) { }
    }

    private static final class TrackingContainer extends FixedComponent {
        int focusWithinLost;

        @Override public void onFocusWithinLost() {
            focusWithinLost++;
            super.onFocusWithinLost();
        }

        void reorderChildren(List<UIComponent> ordered) {
            children.clear();
            children.addAll(ordered);
        }
    }

    private static final class RootKeyCapture extends FixedComponent {
        int keyPresses;

        @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
            keyPresses++;
            return true;
        }
    }

    private static final class TrackingComponent extends FixedComponent {
        int focusLost;
        int keyPresses;
        int typedCharacters;

        TrackingComponent() { focusable(true); }

        @Override public void onFocusLost() {
            focusLost++;
            super.onFocusLost();
        }

        @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
            keyPresses++;
            return true;
        }

        @Override public boolean acceptsTextInput() { return true; }

        @Override public boolean charTyped(char character, int modifiers) {
            typedCharacters++;
            return true;
        }

        void severParentForTest() { parent = null; }
    }
}
