package com.nstut.openui.dnd;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DragAndDropTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void draggingAndDroppingDispatchesDropEventToTargetThroughRuntime() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        AtomicReference<String> droppedItem = new AtomicReference<>();

        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Drop Here"))
                .onDrop(event -> droppedItem.set(event.data()));

        UIComponent root = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 300; }
            @Override public int preferredHeight(Font font) { return 200; }
            @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
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
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);

        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Drop Here"));

        UIComponent root = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 300; }
            @Override public int preferredHeight(Font font) { return 200; }
            @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
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
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        DragTarget<String> target = new DragTarget<>(String.class, Ui.text("Reject Here"))
                .accept(data -> false);
        UIComponent root = new UIComponent() {
            @Override public int preferredWidth(Font ignored) { return 300; }
            @Override public int preferredHeight(Font ignored) { return 200; }
            @Override public void render(GuiGraphicsExtractor g, Font ignored, int mx, int my, float pt) { }
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
    void dragFeedbackMovesWithoutRelayingOutRoot() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        AtomicInteger rootLayouts = new AtomicInteger();
        UIComponent root = new UIComponent() {
            { addChild(source); }
            @Override public int preferredWidth(Font ignored) { return 300; }
            @Override public int preferredHeight(Font ignored) { return 200; }
            @Override public void layout(int x, int y, int width, int height) {
                rootLayouts.incrementAndGet();
                setBounds(x, y, width, height);
                source.layoutTree(font, 10, 10, 60, 20);
            }
            @Override public void render(GuiGraphicsExtractor g, Font ignored, int mx, int my, float pt) { }
        };
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 300, 200);
        runtime.mouseClicked(15, 15, 0);
        runtime.mouseDragged(100, 60, 0, 85, 45);
        runtime.mouseScrolled(100, 60, 0);
        UIComponent feedback = runtime.overlays().components().get(0).children().get(0);
        int firstX = feedback.getX();

        int layoutsAfterFirstMove = rootLayouts.get();

        runtime.mouseDragged(220, 60, 0, 120, 0);
        runtime.mouseScrolled(220, 60, 0);

        assertTrue(feedback.getX() > firstX, "Feedback child should move with the second pointer position");
        assertEquals(layoutsAfterFirstMove, rootLayouts.get(), "Moving feedback must not lay out the root tree");
    }

    @Test
    void rightClickDoesNotCapturePointerForDragging() {
        Font font = new Font(null);
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Draggable<String> source = new Draggable<>("Item-123", Ui.text("Drag Me"));
        AtomicBoolean destinationDragged = new AtomicBoolean();
        UIComponent destination = new UIComponent() {
            @Override public int preferredWidth(Font ignored) { return 80; }
            @Override public int preferredHeight(Font ignored) { return 40; }
            @Override public void render(GuiGraphicsExtractor g, Font ignored, int mx, int my, float pt) { }
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

    private static class DragBox extends UIComponent {
        @Override public int preferredWidth(Font font) { return 80; }
        @Override public int preferredHeight(Font font) { return 20; }
        @Override public void render(GuiGraphicsExtractor g,Font font,int mx,int my,float tick) { }
    }
    private final class DragFixture implements AutoCloseable {
        final UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        final UIComponent root = new DragBox();
        final Draggable<String> source = new Draggable<>("slide", new DragBox());
        final AtomicInteger clicks = new AtomicInteger();
        final UIComponent button = new DragBox() {
            @Override public boolean mouseClicked(double x,double y,int button) { clicks.incrementAndGet();return true; }
        };
        DragFixture() {
            root.addChild(source);root.addChild(button);runtime.setRoot(root);runtime.setViewport(0,0,300,200);
            root.layout(0,0,300,200);source.layout(10,10,80,20);button.layout(150,10,80,30);
        }
        void start() {
            assertTrue(runtime.mouseClicked(15,15,0));runtime.mouseDragged(160,20,0,145,5);
            assertTrue(source.isDragging());assertEquals(1,runtime.overlays().size());
        }
        void clickButton() {runtime.mouseClicked(160,20,0);runtime.mouseReleased(160,20,0);assertEquals(1,clicks.get());}
        public void close() {runtime.close();}
    }
    @Test void unrelatedButtonReleaseDoesNotOrphanTheDragPreview() {
        try(var f=new DragFixture()) {
            f.start();f.runtime.mouseReleased(160,20,1);
            assertTrue(f.source.isDragging(),"Right release must not end a left drag");
            f.runtime.mouseReleased(160,20,0);
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());f.clickButton();
        }
    }
    @Test void removingTheSourceDuringDragCancelsItsPreview() {
        try(var f=new DragFixture()) {
            f.start();f.root.removeChild(f.source);
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());f.clickButton();
        }
    }
    @Test void escapeCancelsTheDragWithoutDroppingOrRearming() {
        try(var f=new DragFixture()) {
            f.start();assertTrue(f.runtime.keyPressed(256,0,0));
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());
            f.source.mouseDragged(160,20,0,10,0);assertFalse(f.source.isDragging());f.clickButton();
        }
    }
    @Test void aNewPressRecoversAfterAMissedRelease() {
        try(var f=new DragFixture()) {
            f.start();f.runtime.overlays().layout(new Font(null),0,0,300,200);
            f.clickButton();assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());
        }
    }
    @Test void previewDoesNotInterceptMouseInput() {
        try(var f=new DragFixture()) {
            f.start();f.runtime.overlays().layout(new Font(null),0,0,300,200);
            assertNull(f.runtime.overlays().hitTest(160,20));
        }
    }

    @Test void throwingDropStillReleasesTheMouseAndPreview() {
        try(var f=new DragFixture()) {
            DragTarget<String> target=new DragTarget<>(String.class,new DragBox()).onDrop(event->{throw new IllegalStateException("Rejected drop");});
            f.root.addChild(target);target.layout(150,10,80,30);f.start();
            assertThrows(IllegalStateException.class,()->f.runtime.mouseReleased(160,20,0));
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());
            f.root.removeChild(target);f.clickButton();
        }
    }
    @Test void replacingTheRootCancelsTheOldDrag() {
        try(var f=new DragFixture()) {
            f.start();f.runtime.setRoot(new DragBox());
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());
        }
    }
    @Test void closingTheRuntimeCancelsTheDrag() {
        try(var f=new DragFixture()) {
            f.start();f.runtime.close();
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());
        }
    }
    @Test void aDragRequiresAFreshLeftPress() {
        try(var f=new DragFixture()) {
            assertFalse(f.source.mouseDragged(160,20,0,10,0));
            assertFalse(f.source.isDragging());assertEquals(0,f.runtime.overlays().size());
        }
    }
    @Test void preventedReleaseCancelsTheDragWithoutDropping() {
        try (var f = new DragFixture()) {
            f.source.on(com.nstut.openui.input.EventType.MOUSE_UP, event -> event.preventDefault());
            f.start();
            f.runtime.mouseReleased(160, 20, 0);
            assertFalse(f.source.isDragging());
            assertEquals(0, f.runtime.overlays().size());
            f.source.mouseDragged(160, 20, 0, 10, 0);
            assertFalse(f.source.isDragging());
            f.clickButton();
        }
    }

    @Test void throwingReleaseListenerStillCancelsTheDrag() {
        try (var f = new DragFixture()) {
            f.source.on(com.nstut.openui.input.EventType.MOUSE_UP, event -> { throw new IllegalStateException("Interrupted release"); });
            f.start();
            assertThrows(IllegalStateException.class, () -> f.runtime.mouseReleased(160, 20, 0));
            assertFalse(f.source.isDragging());
            assertEquals(0, f.runtime.overlays().size());
            f.clickButton();
        }
    }
}
