package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.SizedBox;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopoverTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) { }
        @Override public void remove(AbstractWidget widget) { }
    };

    @Test
    void matchAnchorWidthUsesLaidOutAnchorWidth() {
        UIComponent anchor = new SizedBox(180, 18);
        anchor.layout(20, 20, 180, 18);
        VirtualList<String> results = Ui.list(Signals.of(List.of("Copper Ingot")), Ui::text)
                .itemHeight(20);
        Popover popover = Ui.popover(anchor, results).matchAnchorWidth();

        popover.layoutTree(new Font(null), 0, 0, 320, 200);

        assertEquals(anchor.getWidth(), popover.getWidth());
        assertEquals(anchor.getWidth() - 12, results.getWidth());
    }

    @Test
    void oversizedContentReservesViewportMarginsAndPopoverPadding() {
        UIComponent anchor = new SizedBox(80, 18);
        anchor.layout(20, 20, 80, 18);
        Card oversized = new Card();
        oversized.width(80).height(500);
        Popover popover = Ui.popover(anchor, oversized);

        popover.layoutTree(new Font(null), 0, 0, 200, 120);

        assertEquals(112, popover.getHeight());
        assertEquals(100, oversized.getHeight());
        assertEquals(4, popover.getY());
    }

    @Test
    void samePopoverRemountsWithLatestReactiveRowsAfterClose() {
        Signal<List<String>> items = Signals.of(List.of("Iron", "Copper"));
        UIComponent anchor = new SizedBox(180, 18);
        VirtualList<String> results = Ui.list(items, Ui::text).itemHeight(20);
        Popover popover = Ui.popover(anchor, results).matchAnchorWidth();
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);

        OverlayHandle first = popover.show(runtime.overlays());
        runtime.overlays().layout(new Font(null), 0, 0, 320, 200);
        assertEquals(2, results.activeCellCount());

        items.set(List.of());
        first.close();
        assertFalse(first.isOpen());
        assertEquals(1, popover.childCount());

        items.set(List.of("Rich Slag"));
        OverlayHandle second = popover.show(runtime.overlays());
        runtime.overlays().layout(new Font(null), 0, 0, 320, 200);

        assertTrue(second.isOpen());
        assertEquals(1, results.activeCellCount());
    }
}
