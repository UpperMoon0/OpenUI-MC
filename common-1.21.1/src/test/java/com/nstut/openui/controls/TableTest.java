package com.nstut.openui.controls;

import com.nstut.openui.api.TextWidget;
import com.nstut.openui.api.Ui;
import com.nstut.openui.state.SelectionModel;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    record Item(String name, int score) {}

    @Test
    void sortingSelectionSelectsCorrectItemFromSortedOrder() {
        Signal<List<Item>> items = Signals.of(List.of(
                new Item("Charlie", 10),
                new Item("Alice", 90),
                new Item("Bob", 50)
        ));

        SelectionModel<Item> selection = SelectionModel.single();
        Table<Item> table = Ui.table(items)
                .column(Component.literal("Name"), 0, 1.0F, item -> Ui.text(item.name()), Comparator.comparing(Item::name))
                .selection(selection);

        table.layout(0, 0, 200, 100);

        // Click sort header on column 0 (x = 50, y = 5)
        table.mouseClicked(50, 5, 0);

        List<Item> sorted = table.getSortedItems();
        assertEquals("Alice", sorted.get(0).name());
        assertEquals("Bob", sorted.get(1).name());
        assertEquals("Charlie", sorted.get(2).name());

        // Click on first row (y = 25)
        // Row 0 in sorted table is Alice (was index 1 in raw items)
        table.mouseClicked(20, 25, 0);

        assertTrue(selection.isSelected(sorted.get(0)), "Alice should be selected");
        assertEquals("Alice", selection.selection().get().iterator().next().name());
    }

    @Test
    void cellsAreRetainedAndMountedAsChildren() {
        Signal<List<Item>> items = Signals.of(List.of(
                new Item("Alpha", 1),
                new Item("Beta", 2)
        ));

        Table<Item> table = Ui.table(items)
                .column("Name", 80, item -> Ui.text(item.name()));

        table.layout(0, 0, 200, 100);

        // Verify cells are attached as children
        assertFalse(table.children().isEmpty(), "Table children should contain retained cell components");
        assertEquals(2, table.children().size());
    }

    @Test
    void retainedCellsUpdateWhenSorted() {
        Signal<List<Item>> items = Signals.of(List.of(
                new Item("Charlie", 10),
                new Item("Alice", 90)
        ));

        Table<Item> table = Ui.table(items)
                .keyExtractor(Item::name)
                .column(Component.literal("Name"), 0, 1.0F, item -> Ui.text(item.name()), Comparator.comparing(Item::name));

        table.layout(0, 0, 200, 100);

        // Before sort, top row cell is Charlie
        TextWidget topCellBefore = (TextWidget) table.children().get(0);
        assertEquals("Charlie", topCellBefore.getText().getString());

        // Click sort header to sort ascending
        table.mouseClicked(50, 5, 0);
        table.layout(0, 0, 200, 100);

        // After sort, top row cell must be Alice!
        TextWidget topCellAfter = (TextWidget) table.children().get(0);
        assertEquals("Alice", topCellAfter.getText().getString());
    }
}
