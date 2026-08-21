package com.nstut.openui.dnd;

import com.nstut.openui.api.HStack;
import com.nstut.openui.api.Ui;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DragAndDropTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void draggingAndDroppingDispatchesDropEventToTarget() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        AtomicReference<String> droppedItem = new AtomicReference<>();

        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Drop Here"))
                .onDrop(event -> droppedItem.set(event.data()));

        HStack root = Ui.row(source, target);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        root.layout(0, 0, 300, 200);

        source.layout(10, 10, 60, 20);
        target.layout(150, 10, 80, 40);

        // Click source
        source.mouseClicked(15, 15, 0);

        // Drag past threshold to target coordinates
        source.mouseDragged(160, 20, 0, 145, 5);
        assertTrue(source.isDragging(), "Source should enter dragging state");

        // Release over target
        source.mouseReleased(160, 20, 0);
        assertFalse(source.isDragging(), "Source should end dragging state");

        assertEquals("Item-123", droppedItem.get(), "Target should have received dropped data");
    }
}
