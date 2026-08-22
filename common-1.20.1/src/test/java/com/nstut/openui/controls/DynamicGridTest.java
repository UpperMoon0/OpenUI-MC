package com.nstut.openui.controls;

import com.nstut.openui.api.Ui;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DynamicGridTest {
    @Test
    void measurementAccountsForAllRows() {
        Signal<List<String>> items = Signals.of(List.of("A", "B", "C", "D", "E"));
        DynamicGrid<String> grid = Ui.grid(items, Ui::text).minCellWidth(120).cellHeight(44).gap(4);
        var measured = grid.measure(Constraints.loose(272, 1000), null);
        assertEquals(140, measured.height()); // 3 rows * 44 + 2 gaps * 4
    }
}
