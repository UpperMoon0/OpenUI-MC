package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.VStack;
import com.nstut.openui.debug.LayoutDiagnostics;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class BoundedOverflowTest {
    private static final class FixedBox extends UIComponent {
        private final int preferredWidth;
        private final int preferredHeight;

        private FixedBox(int preferredWidth, int preferredHeight) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        @Override public int preferredWidth(Font font) { return preferredWidth; }
        @Override public int preferredHeight(Font font) { return preferredHeight; }
        @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) { }
    }

    private static final class InspectableCard extends Card {
        private InspectableCard(UIComponent child) { super(child); }
        boolean clipsChildren() { return clipsChildrenToBounds(); }
        int clipX() { return childClipX(); }
        int clipY() { return childClipY(); }
        int clipWidth() { return childClipWidth(); }
        int clipHeight() { return childClipHeight(); }
    }

    private static final class InspectableStack extends VStack {
        boolean clipsChildren() { return clipsChildrenToBounds(); }
    }

    private static final class InspectableScrollView extends ScrollView {
        private InspectableScrollView(UIComponent content) { super(content); }
        boolean clipsChildren() { return clipsChildrenToBounds(); }
        int clipX() { return childClipX(); }
        int clipY() { return childClipY(); }
        int clipWidth() { return childClipWidth(); }
        int clipHeight() { return childClipHeight(); }
    }

    private static final NativeWidgetHost DUMMY_HOST = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) { }
        @Override public void remove(AbstractWidget widget) { }
    };

    private static Font font() { return new Font(null, false); }

    @Test
    void boundedCardClipsOversizedChildAndReportsDebugOverflow() {
        FixedBox child = new FixedBox(80, 300);
        InspectableCard card = new InspectableCard(child);
        card.padding(4);

        ByteArrayOutputStream warning = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        System.setErr(new PrintStream(warning));
        LayoutDiagnostics.openDebugSession();
        try {
            card.layoutTree(font(), 10, 20, 100, 60);
        } finally {
            LayoutDiagnostics.closeDebugSession();
            System.setErr(previousErr);
        }

        assertTrue(card.clipsChildren());
        assertEquals(10, card.clipX());
        assertEquals(20, card.clipY());
        assertEquals(100, card.clipWidth());
        assertEquals(60, card.clipHeight());
        assertEquals(52, child.getHeight(), "layout remains bounded instead of silently growing the card");
        assertTrue(warning.toString().contains("Content height exceeds bounded parent; consider Ui.scroll(...)"));
    }

    @Test
    void unboundedNormalStackKeepsNaturalLayoutAndDoesNotClip() {
        FixedBox child = new FixedBox(80, 300);
        InspectableStack stack = new InspectableStack();
        stack.addChild(child);

        assertEquals(300, stack.measure(Constraints.loose(100, Constraints.INFINITY), font()).height());
        stack.layoutTree(font(), 0, 0, 100, 300);

        assertFalse(stack.clipsChildren());
        assertEquals(300, child.getHeight());
        assertFalse(stack.mouseScrolled(10, 10, -1), "normal stacks must not become implicit scroll views");
    }

    @Test
    void explicitScrollStillClipsAndScrolls() {
        FixedBox content = new FixedBox(80, 300);
        InspectableScrollView scroll = new InspectableScrollView(content);
        scroll.layoutTree(font(), 5, 7, 100, 80);

        assertTrue(scroll.clipsChildren());
        assertEquals(5, scroll.clipX());
        assertEquals(7, scroll.clipY());
        assertEquals(100, scroll.clipWidth());
        assertEquals(80, scroll.clipHeight());
        assertEquals(300, content.getHeight());
        assertEquals(7, content.getY());

        assertTrue(scroll.mouseScrolled(10, 10, -1));
        scroll.layoutTree(font(), 5, 7, 100, 80);
        assertEquals(-17, content.getY(), "one wheel step should move the explicit scroll content by 24px");
    }

    @Test
    void popoverFromClippedCardRendersAsIndependentOverlayRoot() {
        FixedBox anchor = new FixedBox(80, 20);
        InspectableCard card = new InspectableCard(anchor);
        card.padding(4);
        UiRuntime runtime = new UiRuntime(font(), DUMMY_HOST);
        try {
            runtime.setRoot(card);
            card.layoutTree(font(), 0, 0, 120, 60);

            Popover popover = Ui.popover(anchor, new FixedBox(60, 20));
            OverlayHandle handle = popover.show(runtime.overlays());
            runtime.overlays().layout(font(), 0, 0, 320, 200);

            assertTrue(handle.isOpen());
            assertNull(popover.parent(), "overlay roots must not become descendants of the clipped anchor surface");
            assertTrue(runtime.overlays().containsComponent(popover));
            assertTrue(popover.getY() >= card.getY() + card.getHeight(),
                    "popover should be free to render beyond the card through the overlay layer");
            handle.close();
        } finally {
            runtime.close();
        }
    }

    @Test
    void nestedBoundedSurfacesExposeIndependentClipAncestors() {
        FixedBox oversized = new FixedBox(100, 400);
        InspectableCard inner = new InspectableCard(oversized);
        inner.padding(3);
        InspectableCard outer = new InspectableCard(inner);
        outer.padding(5);

        outer.layoutTree(font(), 10, 20, 120, 90);

        assertTrue(outer.clipsChildren());
        assertTrue(inner.clipsChildren());
        assertEquals(10, outer.clipX());
        assertEquals(20, outer.clipY());
        assertEquals(120, outer.clipWidth());
        assertEquals(90, outer.clipHeight());
        assertEquals(15, inner.clipX());
        assertEquals(25, inner.clipY());
        assertEquals(110, inner.clipWidth());
        assertEquals(80, inner.clipHeight());
        assertTrue(inner.clipX() >= outer.clipX());
        assertTrue(inner.clipY() >= outer.clipY());
        assertTrue(inner.clipX() + inner.clipWidth() <= outer.clipX() + outer.clipWidth());
        assertTrue(inner.clipY() + inner.clipHeight() <= outer.clipY() + outer.clipHeight());
    }
}
