package com.nstut.openui.dnd;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DragAndDropTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void draggingAndDroppingDispatchesDropEventToTargetThroughRuntime() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        AtomicReference<String> droppedItem = new AtomicReference<>();

        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Drop Here"))
                .onDrop(event -> droppedItem.set(event.data()));

        UIComponent root = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 300; }
            @Override public int preferredHeight(Font font) { return 200; }
            @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
                for (UIComponent c : children) c.render(g, font, mx, my, pt);
            }
        };
        root.addChild(source);
        root.addChild(target);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        root.layout(0, 0, 300, 200);
        source.layout(10, 10, 60, 20);
        target.layout(150, 10, 80, 40);

        runtime.mouseClicked(15, 15, 0);
        runtime.mouseDragged(160, 20, 0, 145, 5);
        assertTrue(source.isDragging(), "Source should enter dragging state after runtime drag");

        runtime.mouseReleased(160, 20, 0);
        assertFalse(source.isDragging(), "Source should end dragging state after runtime release");

        assertEquals("Item-123", droppedItem.get(), "Target should have received dropped data via runtime path");
    }

    @Test
    void dragTargetReceivesDragOverHighlightDuringRuntimeDrag() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Drop Here"));

        UIComponent root = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 300; }
            @Override public int preferredHeight(Font font) { return 200; }
            @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
                for (UIComponent c : children) c.render(g, font, mx, my, pt);
            }
        };
        root.addChild(source);
        root.addChild(target);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        root.layout(0, 0, 300, 200);
        source.layout(10, 10, 60, 20);
        target.layout(150, 10, 80, 40);

        runtime.mouseClicked(15, 15, 0);
        assertFalse(target.isDragOver(), "Target should not have dragOver before drag starts");

        runtime.mouseDragged(160, 20, 0, 145, 5);
        assertTrue(target.isDragOver(), "Target should have dragOver during runtime drag over it");

        runtime.mouseReleased(160, 20, 0);
        assertFalse(target.isDragOver(), "Target should lose dragOver after release");
    }

    @Test
    void rejectingTargetDoesNotReceiveDragOverHighlight() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Reject Here"))
                .accept(data -> false);
        UIComponent root = new UIComponent() {
            @Override public int preferredWidth(Font ignored) { return 300; }
            @Override public int preferredHeight(Font ignored) { return 200; }
            @Override public void render(GuiGraphics g, Font ignored, int mx, int my, float pt) { }
        };
        root.addChild(source);
        root.addChild(target);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        root.layout(0, 0, 300, 200);
        source.layout(10, 10, 60, 20);
        target.layout(150, 10, 80, 40);

        runtime.mouseClicked(15, 15, 0);
        runtime.mouseDragged(160, 20, 0, 145, 5);

        assertFalse(target.isDragOver(), "Rejecting targets must not advertise a valid drop");
    }

    @Test
    void dragFeedbackRequestsLayoutAndMovesToLatestPointer() throws Exception {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        UIComponent root = Ui.stack(source);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        root.layout(0, 0, 300, 200);
        source.layout(10, 10, 60, 20);

        runtime.mouseClicked(15, 15, 0);
        runtime.mouseDragged(100, 60, 0, 85, 45);
        runtime.overlays().layout(font, 0, 0, 300, 200);
        UIComponent feedback = runtime.overlays().components().get(0).children().get(0);
        int firstX = feedback.getX();

        Field layoutDirty = UiRuntime.class.getDeclaredField("layoutDirty");
        layoutDirty.setAccessible(true);
        layoutDirty.setBoolean(runtime, false);

        runtime.mouseDragged(220, 60, 0, 120, 0);
        assertTrue(layoutDirty.getBoolean(runtime), "Moving feedback must invalidate layout");
        runtime.overlays().layout(font, 0, 0, 300, 200);

        assertTrue(feedback.getX() > firstX, "Feedback child should move with the second pointer position");
    }

    @Test
    void rightClickDoesNotCapturePointerForDragging() {
        Font font = new Font(null, false);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        AtomicBoolean destinationDragged = new AtomicBoolean();
        UIComponent destination = new UIComponent() {
            @Override public int preferredWidth(Font ignored) { return 80; }
            @Override public int preferredHeight(Font ignored) { return 40; }
            @Override public void render(GuiGraphics g, Font ignored, int mx, int my, float pt) { }
            @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
                destinationDragged.set(true);
                return true;
            }
        };
        UIComponent root = Ui.stack(source, destination);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        root.layout(0, 0, 300, 200);
        source.layout(10, 10, 60, 20);
        destination.layout(150, 10, 80, 40);

        runtime.mouseClicked(15, 15, 1);
        runtime.mouseDragged(160, 20, 1, 145, 5);

        assertFalse(source.isDragging());
        assertTrue(destinationDragged.get(), "Right-button drag should route to the component under the pointer");
    }
}
