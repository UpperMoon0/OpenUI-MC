package com.nstut.openui.api;

import com.nstut.openui.controls.SignalSwitcher;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LayoutRegressionTest {

    private static final class FixedBox extends UIComponent {
        private final int w, h;
        FixedBox(int w, int h) {
            this.w = w;
            this.h = h;
        }
        @Override public int preferredWidth(Font font) { return w; }
        @Override public int preferredHeight(Font font) { return h; }
        @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {}
    }

    @Test
    void hStackFixedChildrenDoNotShrinkAndFlexTakesRemaining() {
        HStack row = new HStack().gap(10);
        UIComponent fixed1 = new FixedBox(50, 20);
        UIComponent flexChild = new FixedBox(10, 20).flex();
        UIComponent fixed2 = new FixedBox(30, 20);

        row.addChild(fixed1);
        row.addChild(flexChild);
        row.addChild(fixed2);

        row.layout(0, 0, 200, 40);

        assertEquals(50, fixed1.getWidth());
        assertEquals(100, flexChild.getWidth());
        assertEquals(30, fixed2.getWidth());

        assertEquals(0, fixed1.getX());
        assertEquals(60, flexChild.getX());
        assertEquals(170, fixed2.getX());
    }

    @Test
    void vStackOnlyFlexChildConsumesRemainingHeight() {
        VStack col = new VStack().gap(5);
        UIComponent fixedHeader = new FixedBox(100, 30);
        UIComponent flexContent = new FixedBox(100, 10).flex();
        UIComponent fixedFooter = new FixedBox(100, 20);

        col.addChild(fixedHeader);
        col.addChild(flexContent);
        col.addChild(fixedFooter);

        col.layout(0, 0, 100, 150);

        assertEquals(30, fixedHeader.getHeight());
        assertEquals(90, flexContent.getHeight());
        assertEquals(20, fixedFooter.getHeight());
    }

    @Test
    void stackStretchesRoutedChildToAllocatedBounds() {
        Stack stack = new Stack();
        UIComponent child = new FixedBox(20, 20);
        stack.addChild(child);

        stack.layout(10, 20, 200, 150);

        assertEquals(10, child.getX());
        assertEquals(20, child.getY());
        assertEquals(200, child.getWidth());
        assertEquals(150, child.getHeight());
    }

    @Test
    void signalSwitcherChildReceivesFullAllocatedBounds() {
        Signal<String> route = Signals.of("TAB1");
        FixedBox tab1Content = new FixedBox(50, 50);
        FixedBox tab2Content = new FixedBox(50, 50);

        SignalSwitcher<String> switcher = Ui.switcher(route)
                .when("TAB1", () -> tab1Content)
                .when("TAB2", () -> tab2Content);

        switcher.layout(15, 25, 300, 200);

        assertEquals(15, tab1Content.getX());
        assertEquals(25, tab1Content.getY());
        assertEquals(300, tab1Content.getWidth());
        assertEquals(200, tab1Content.getHeight());
    }

    @Test
    void signalSwitcherBuildsInitiallySelectedLaterRoute() {
        Signal<String> route = Signals.of("TAB2");
        FixedBox tab2Content = new FixedBox(50, 50);

        SignalSwitcher<String> switcher = Ui.switcher(route)
                .when("TAB1", () -> new FixedBox(50, 50))
                .when("TAB2", () -> tab2Content);

        switcher.layout(15, 25, 300, 200);

        assertEquals(1, switcher.children().size());
        assertSame(tab2Content, switcher.children().get(0));
        assertEquals(300, tab2Content.getWidth());
        assertEquals(200, tab2Content.getHeight());
    }

    @Test
    void explicitlySizedHorizontalSpacerRemainsFixed() {
        HStack row = new HStack();
        UIComponent leading = new FixedBox(10, 10);
        UIComponent spacer = new Spacer().width(18);
        UIComponent flexible = new FixedBox(10, 10).flex();
        row.addChild(leading);
        row.addChild(spacer);
        row.addChild(flexible);

        assertEquals(38, row.preferredWidth(null));
        row.layout(0, 0, 100, 10);

        assertEquals(18, spacer.getWidth());
        assertEquals(72, flexible.getWidth());
    }

    @Test
    void implicitHorizontalSpacerConsumesRemainingWidth() {
        HStack row = new HStack();
        UIComponent leading = new FixedBox(10, 10);
        UIComponent spacer = new Spacer();
        UIComponent trailing = new FixedBox(10, 10);
        row.addChild(leading);
        row.addChild(spacer);
        row.addChild(trailing);

        row.layout(0, 0, 100, 10);

        assertEquals(80, spacer.getWidth());
    }

    @Test
    void explicitlySizedVerticalSpacerRemainsFixed() {
        VStack column = new VStack();
        UIComponent leading = new FixedBox(10, 10);
        UIComponent spacer = new Spacer().height(18);
        UIComponent flexible = new FixedBox(10, 10).flex();
        column.addChild(leading);
        column.addChild(spacer);
        column.addChild(flexible);

        assertEquals(38, column.preferredHeight(null));
        column.layout(0, 0, 10, 100);

        assertEquals(18, spacer.getHeight());
        assertEquals(72, flexible.getHeight());
    }
}
