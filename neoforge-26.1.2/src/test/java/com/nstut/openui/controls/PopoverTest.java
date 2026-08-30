package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.SizedBox;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopoverTest {

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
}
