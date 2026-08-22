package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicGridTest {

    private static class SimpleCard extends UIComponent {
        final String item;
        SimpleCard(String item) {
            this.item = item;
        }
        @Override public int preferredWidth(Font font) { return 120; }
        @Override public int preferredHeight(Font font) { return 44; }
        @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {}
    }

    @Test
    void testDynamicGridIntrinsicMeasurement() {
        Signal<List<String>> items = Signals.of(List.of("1", "2", "3", "4", "5"));
        DynamicGrid<String> grid = Ui.grid(items, SimpleCard::new)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        Size measured = grid.measure(new Constraints(0, 250, 0, 1000), null);
        assertEquals(140, measured.height());
        assertEquals(250, measured.width());

        Size measuredNarrow = grid.measure(new Constraints(0, 119, 0, 1000), null);
        assertEquals(236, measuredNarrow.height());
    }

    @Test
    void testDynamicGridEmptyStateMeasurement() {
        Signal<List<String>> items = Signals.of(List.of());
        DynamicGrid<String> grid = Ui.grid(items, SimpleCard::new)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        Size measured = grid.measure(new Constraints(0, 250, 0, 1000), null);
        assertEquals(0, measured.height());
    }

    @Test
    void testDynamicGridLayoutPositions() {
        Signal<List<String>> items = Signals.of(List.of("1", "2", "3", "4", "5"));
        DynamicGrid<String> grid = Ui.grid(items, SimpleCard::new)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        grid.layout(10, 20, 244, 200);
        assertEquals(2, grid.columns());

        assertEquals(10, grid.children().get(0).getX());
        assertEquals(20, grid.children().get(0).getY());

        assertEquals(10 + 120 + 4, grid.children().get(1).getX());
        assertEquals(20, grid.children().get(1).getY());

        assertEquals(10, grid.children().get(2).getX());
        assertEquals(20 + 44 + 4, grid.children().get(2).getY());
    }
}
