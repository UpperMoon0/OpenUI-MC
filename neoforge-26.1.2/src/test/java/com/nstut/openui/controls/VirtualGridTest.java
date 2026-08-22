package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VirtualGridTest {

    private static class DummyItem {
        final String id;
        final String text;
        DummyItem(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    private static class SimpleCard extends UIComponent {
        final DummyItem item;
        SimpleCard(DummyItem item) {
            this.item = item;
        }
        @Override public int preferredWidth(Font font) { return 100; }
        @Override public int preferredHeight(Font font) { return 40; }
        @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {}
    }

    @Test
    void testColumnsCalculation() {
        Signal<List<DummyItem>> items = Signals.of(List.of());
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        // 272px width: (272 + 4) / (120 + 4) = 276 / 124 = 2 columns
        grid.layout(0, 0, 272, 200);
        assertEquals(2, grid.columns());

        // 119px width: (119 + 4) / (120 + 4) = 123 / 124 = 0 -> clamped to 1
        grid.layout(0, 0, 119, 200);
        assertEquals(1, grid.columns());
    }

    @Test
    void testRowCountAndFiveItemsInTwoColumns() {
        List<DummyItem> list = List.of(
                new DummyItem("1", "A"),
                new DummyItem("2", "B"),
                new DummyItem("3", "C"),
                new DummyItem("4", "D"),
                new DummyItem("5", "E")
        );
        Signal<List<DummyItem>> items = Signals.of(list);
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new)
                .key(i -> i.id)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        grid.layout(0, 0, 272, 300);
        assertEquals(2, grid.columns());
        assertEquals(5, grid.activeCellCount());
    }

    @Test
    void testVirtualizationWithThousandItems() {
        List<DummyItem> bigList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            bigList.add(new DummyItem(String.valueOf(i), "Item " + i));
        }
        Signal<List<DummyItem>> items = Signals.of(bigList);
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new)
                .key(i -> i.id)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4)
                .overscanRows(1);

        grid.layout(0, 0, 272, 100);
        assertEquals(2, grid.columns());
        assertTrue(grid.activeCellCount() <= 12, "Active cells (" + grid.activeCellCount() + ") should be bounded");
        assertTrue(grid.activeCellCount() >= 4);
    }

    @Test
    void testScrollClampingAndLimits() {
        List<DummyItem> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) list.add(new DummyItem(String.valueOf(i), "Item " + i));

        Signal<List<DummyItem>> items = Signals.of(list);
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        grid.layout(0, 0, 272, 100);

        // Scroll up past zero
        grid.mouseScrolled(10, 10, 10.0);
        assertEquals(0.0, grid.scrollOffset(), 0.001);

        // Scroll down a lot
        grid.mouseScrolled(10, 10, -100.0);
        assertEquals(376.0, grid.scrollOffset(), 0.001);

        // Reset scroll
        grid.resetScroll();
        assertEquals(0.0, grid.scrollOffset(), 0.001);
    }

    private static final com.nstut.openui.runtime.NativeWidgetHost DUMMY_HOST = new com.nstut.openui.runtime.NativeWidgetHost() {
        @Override public void add(net.minecraft.client.gui.components.AbstractWidget widget) {}
        @Override public void remove(net.minecraft.client.gui.components.AbstractWidget widget) {}
    };

    @Test
    void testSignalReplacingItemListClampsScroll() {
        List<DummyItem> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) list.add(new DummyItem(String.valueOf(i), "Item " + i));

        Signal<List<DummyItem>> items = Signals.of(list);
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        UiRuntime runtime = new UiRuntime(new Font(null), DUMMY_HOST);
        grid.mount(runtime);

        grid.layout(0, 0, 272, 100);
        grid.mouseScrolled(10, 10, -50.0);
        assertTrue(grid.scrollOffset() > 100);

        // Replace with 2 items -> max scroll becomes 0
        items.set(List.of(new DummyItem("1", "A"), new DummyItem("2", "B")));
        grid.layout(0, 0, 272, 100);
        assertEquals(0.0, grid.scrollOffset(), 0.001);

        grid.unmount();
    }

    @Test
    void testStableKeysReuseCells() {
        DummyItem first = new DummyItem("1", "A");
        Signal<List<DummyItem>> items = Signals.of(List.of(
                first,
                new DummyItem("2", "B"),
                new DummyItem("3", "C")
        ));
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new)
                .key(i -> i.id)
                .minCellWidth(120)
                .cellHeight(44)
                .gap(4);

        UiRuntime runtime = new UiRuntime(new Font(null), DUMMY_HOST);
        grid.mount(runtime);
        grid.layout(0, 0, 272, 200);

        UIComponent cell1 = grid.children().get(0);

        // Reusing the unchanged item under the same key preserves its cell.
        items.set(List.of(
                first,
                new DummyItem("4", "D"),
                new DummyItem("3", "C")
        ));
        grid.layout(0, 0, 272, 200);

        assertSame(cell1, grid.children().get(0), "Cell with key '1' should be reused");
        grid.unmount();
    }

    @Test
    void testEmptyListHasNoActiveCells() {
        Signal<List<DummyItem>> items = Signals.of(List.of());
        VirtualGrid<DummyItem> grid = Ui.virtualGrid(items, SimpleCard::new);
        grid.layout(0, 0, 200, 200);
        assertEquals(0, grid.activeCellCount());
    }
}
