package com.nstut.openui.controls;

import com.nstut.openui.api.TextWidget;
import com.nstut.openui.api.Ui;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VirtualGridTest {
    @Test
    void resolvesTwoColumnsAtMarketplaceWidth() {
        Signal<List<String>> items = Signals.of(List.of("A", "B", "C", "D", "E"));
        VirtualGrid<String> grid = Ui.virtualGrid(items, Ui::text)
                .key(v -> v)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);
        grid.layout(0, 0, 272, 140);
        assertEquals(2, grid.columns());
        assertEquals(5, grid.activeCellCount());
    }

    @Test
    void virtualizesLargeDataSet() {
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 1000; i++) data.add("Item " + i);
        Signal<List<String>> items = Signals.of(data);
        VirtualGrid<String> grid = Ui.virtualGrid(items, Ui::text)
                .key(v -> v)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4)
                .overscanRows(1);
        grid.layout(0, 0, 272, 140);
        assertTrue(grid.activeCellCount() < 30, "grid must virtualize rather than build all 1000 cells");
        assertTrue(grid.activeCellCount() >= 4);
    }

    @Test
    void widthChangeReflowsColumns() {
        Signal<List<String>> items = Signals.of(List.of("A", "B", "C", "D"));
        VirtualGrid<String> grid = Ui.virtualGrid(items, Ui::text).minCellWidth(120).cellHeight(44).gap(4);
        grid.layout(0, 0, 272, 100);
        assertEquals(2, grid.columns());
        grid.layout(0, 0, 119, 100);
        assertEquals(1, grid.columns());
    }
}
