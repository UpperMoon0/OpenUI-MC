package com.nstut.openui.controls;

import com.nstut.openui.api.Panel;
import com.nstut.openui.api.StyledBox;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.VStack;
import com.nstut.openui.debug.LayoutDiagnostics;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class BoundedOverflowTest {
    private static class FixedBox extends UIComponent {
        private final int preferredWidth;
        private final int preferredHeight;

        private FixedBox(int preferredWidth, int preferredHeight) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        @Override public int preferredWidth(Font font) { return preferredWidth; }
        @Override public int preferredHeight(Font font) { return preferredHeight; }
        @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) { }
    }

    private static final class CountingBox extends FixedBox {
        private int measureCalls;

        private CountingBox(int preferredWidth, int preferredHeight) {
            super(preferredWidth, preferredHeight);
        }

        @Override public Size measure(Constraints constraints, Font font) {
            measureCalls++;
            return super.measure(constraints, font);
        }
    }

    private static final class CountingScrollView extends ScrollView {
        private int measureCalls;

        private CountingScrollView(UIComponent content) { super(content); }

        @Override public Size measure(Constraints constraints, Font font) {
            measureCalls++;
            return super.measure(constraints, font);
        }
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

    private static Font font() { return new Font(null); }

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
        assertEquals(14, card.clipX());
        assertEquals(24, card.clipY());
        assertEquals(92, card.clipWidth());
        assertEquals(52, card.clipHeight());
        assertEquals(52, child.getHeight(), "layout remains bounded instead of silently growing the card");
        assertTrue(warning.toString().contains("Content height exceeds bounded parent; consider Ui.scroll(...)"));
    }

    @Test
    void disabledDiagnosticsDoNotAddProductionMeasurementPasses() {
        CountingBox cardChild = new CountingBox(80, 300);
        new Card(cardChild).padding(4).layoutTree(font(), 0, 0, 100, 60);
        assertEquals(0, cardChild.measureCalls);

        CountingBox panelChild = new CountingBox(80, 300);
        new Panel().padding(4).child(panelChild).layoutTree(font(), 0, 0, 100, 60);
        assertEquals(0, panelChild.measureCalls);

        CountingBox styledChild = new CountingBox(80, 300);
        StyledBox styled = Ui.styled(StateStyle.of(Style.builder().padding(4).build()), styledChild);
        styled.layoutTree(font(), 0, 0, 100, 60);
        assertEquals(0, styledChild.measureCalls);

        CountingScrollView explicitScroll = new CountingScrollView(new FixedBox(80, 300));
        LayoutDiagnostics.openDebugSession();
        try {
            new Card(explicitScroll).padding(4).layoutTree(font(), 0, 0, 100, 60);
        } finally {
            LayoutDiagnostics.closeDebugSession();
        }
        assertEquals(0, explicitScroll.measureCalls,
                "explicit scroll children must be excluded before diagnostic measurement");
    }

    @Test
    void flexScrollSubtreeDoesNotProduceOverflowWarning() {
        UIComponent explicitScroll = Ui.scroll(new FixedBox(80, 300)).flex();
        VStack body = Ui.column(new FixedBox(80, 20), explicitScroll).gap(6);
        Card card = new Card(body).padding(4);

        ByteArrayOutputStream warning = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        System.setErr(new PrintStream(warning));
        LayoutDiagnostics.openDebugSession();
        try {
            card.layoutTree(font(), 0, 0, 100, 100);
        } finally {
            LayoutDiagnostics.closeDebugSession();
            System.setErr(previousErr);
        }

        assertFalse(warning.toString().contains("Content height exceeds bounded parent"),
                "the documented column(header, scroll(tall).flex()) pattern must not be diagnosed as overflow");
        assertTrue(explicitScroll.getHeight() < 300,
                "the flex scroll viewport should consume bounded remaining height rather than its natural content height");
    }

    @Test
    void directFillHeightChildDoesNotProduceOverflowWarning() {
        FixedBox fillChild = new FixedBox(80, 300);
        fillChild.fillHeight();
        Card card = new Card(fillChild).padding(4);

        ByteArrayOutputStream warning = new ByteArrayOutputStream();
        PrintStream previousErr = System.err;
        System.setErr(new PrintStream(warning));
        LayoutDiagnostics.openDebugSession();
        try {
            card.layoutTree(font(), 0, 0, 100, 60);
        } finally {
            LayoutDiagnostics.closeDebugSession();
            System.setErr(previousErr);
        }

        assertFalse(warning.toString().contains("Content height exceeds bounded parent"),
                "fillHeight is constraint-driven and must not be treated as intrinsic overflow");
        assertEquals(52, fillChild.getHeight());
    }

    @Test
    void zeroPaddingBorderedSurfacesAlignChildBoundsAndHitTestingWithProtectedRegion() {
        FixedBox cardChild = new FixedBox(10, 10);
        Card card = new Card(cardChild).padding(0).radius(0).elevated(false);
        card.layoutTree(font(), 0, 0, 20, 20);
        assertEquals(1, cardChild.getX());
        assertEquals(1, cardChild.getY());
        assertEquals(18, cardChild.getWidth());
        assertEquals(18, cardChild.getHeight());
        assertSame(card, card.hitTest(0, 10), "clipped card border strip must not target its child");
        assertSame(cardChild, card.hitTest(1, 10));

        FixedBox panelChild = new FixedBox(10, 10);
        Panel panel = new Panel(0xFF111111, 0xFFEEEEEE).padding(0).radius(0).child(panelChild);
        panel.layoutTree(font(), 0, 0, 20, 20);
        assertEquals(1, panelChild.getX());
        assertEquals(18, panelChild.getWidth());
        assertSame(panel, panel.hitTest(0, 10), "clipped panel border strip must not target its child");
        assertSame(panelChild, panel.hitTest(1, 10));

        FixedBox styledChild = new FixedBox(10, 10);
        Style style = Style.builder()
                .padding(0)
                .background(0xFF111111)
                .border(4, 0xFFEEEEEE)
                .radius(0)
                .build();
        StyledBox styled = Ui.styled(StateStyle.of(style), styledChild);
        styled.layoutTree(font(), 0, 0, 20, 20);
        assertEquals(4, styledChild.getX());
        assertEquals(12, styledChild.getWidth());
        assertSame(styled, styled.hitTest(3, 10), "styled border width must be excluded from child input");
        assertSame(styledChild, styled.hitTest(4, 10));
    }

    @Test
    void cardFocusAndSelectionDoNotReflowProtectedContent() {
        FixedBox child=new FixedBox(10,10);
        Card card=new Card(child).outlined(false).padding(0).radius(0).elevated(false).clickable(true);
        UiRuntime runtime=new UiRuntime(font(),DUMMY_HOST);
        try {
            runtime.setRoot(card);
            card.layoutTree(font(),0,0,20,20);
            int initialX=child.getX(), initialY=child.getY();
            int initialWidth=child.getWidth(), initialHeight=child.getHeight();
            assertEquals(1,initialX,
                    "Card reserves its one-pixel state-border gutter before focus/selection so state paint cannot reflow content");
            assertEquals(18,initialWidth);

            card.requestFocus();
            assertTrue(card.isFocused());
            card.layoutTree(font(),0,0,20,20);
            assertEquals(initialX,child.getX());
            assertEquals(initialY,child.getY());
            assertEquals(initialWidth,child.getWidth());
            assertEquals(initialHeight,child.getHeight());

            card.selected(true);
            card.layoutTree(font(),0,0,20,20);
            assertEquals(initialX,child.getX());
            assertEquals(initialY,child.getY());
            assertEquals(initialWidth,child.getWidth());
            assertEquals(initialHeight,child.getHeight());

            card.clearFocus();
            card.selected(false);
            card.layoutTree(font(),0,0,20,20);
            assertEquals(initialX,child.getX());
            assertEquals(initialWidth,child.getWidth());
        } finally {
            runtime.close();
        }
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
        assertEquals(15, outer.clipX());
        assertEquals(25, outer.clipY());
        assertEquals(110, outer.clipWidth());
        assertEquals(80, outer.clipHeight());
        assertEquals(18, inner.clipX());
        assertEquals(28, inner.clipY());
        assertEquals(104, inner.clipWidth());
        assertEquals(74, inner.clipHeight());
        assertTrue(inner.clipX() >= outer.clipX());
        assertTrue(inner.clipY() >= outer.clipY());
        assertTrue(inner.clipX() + inner.clipWidth() <= outer.clipX() + outer.clipWidth());
        assertTrue(inner.clipY() + inner.clipHeight() <= outer.clipY() + outer.clipHeight());
    }
}
