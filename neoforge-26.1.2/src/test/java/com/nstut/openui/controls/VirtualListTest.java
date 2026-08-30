package com.nstut.openui.controls;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.TextWidget;
import com.nstut.openui.api.Ui;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VirtualListTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) { }
        @Override public void remove(AbstractWidget widget) { }
    };

    @Test
    void mountReplaysSignalChangesPublishedAfterConstruction() {
        Signal<List<String>> items = Signals.of(List.of());
        VirtualList<String> list = Ui.list(items, Ui::text).itemHeight(20);
        items.set(List.of("Copper", "Iron"));

        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        runtime.setRoot(list);
        list.layoutTree(new Font(null), 0, 0, 200, 100);

        assertEquals(2, list.activeCellCount());
    }

    @Test
    void materializedRowsUseTheRealFontOnTheirFirstLayout() {
        Signal<List<String>> items = Signals.of(List.of("row"));
        AtomicReference<ButtonWidget> renderedButton = new AtomicReference<>();
        VirtualList<String> list = Ui.list(items, ignored -> {
            ButtonWidget button = Ui.button("Select", () -> {}).small();
            renderedButton.set(button);
            return Ui.row(Ui.spacer().flex(), button);
        }).itemHeight(24);
        Font font = new Font(null);

        list.layoutTree(font, 0, 0, 200, 24);

        ButtonWidget button = renderedButton.get();
        assertNotNull(button);
        assertEquals(button.preferredWidth(font), button.getWidth(),
                "A virtual row must not use ButtonWidget's null-font fallback on its first frame");
        int firstWidth = button.getWidth();

        list.layoutTree(font, 0, 0, 200, 24);
        assertEquals(firstWidth, button.getWidth(), "Relayout must not resize a materialized row");
    }

    @Test
    void virtualListLimitsActiveChildrenToViewportPlusOverscan() {
        List<String> bigList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            bigList.add("Item " + i);
        }

        Signal<List<String>> items = Signals.of(bigList);
        VirtualList<String> list = Ui.list(items, Ui::text)
                .itemHeight(20)
                .gap(0)
                .overscan(2);

        // Viewport height: 100px -> 5 visible items. With overscan 2 on top/bottom -> ~9 active items.
        list.layout(0, 0, 200, 100);

        assertTrue(list.activeCellCount() <= 10, "Active cells (" + list.activeCellCount() + ") should be bounded by viewport + overscan, not 1000");
        assertTrue(list.activeCellCount() >= 5);
    }

    @Test
    void virtualListReusesKeysWhenDataChanges() {
        Signal<List<String>> items = Signals.of(List.of("A", "B", "C"));
        VirtualList<String> list = Ui.list(items, Ui::text)
                .key(s -> s)
                .itemHeight(20);

        list.layout(0, 0, 200, 100);
        assertEquals(3, list.activeCellCount());

        // Update list to A, D, C (A and C should be preserved)
        items.set(List.of("A", "D", "C"));
        list.layout(0, 0, 200, 100);
        assertEquals(3, list.activeCellCount());
    }
}
