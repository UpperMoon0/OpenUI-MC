package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrollbarDragTest {
    private static class Content extends UIComponent {
        int clicks;
        @Override public int preferredWidth(Font font) { return 100; }
        @Override public int preferredHeight(Font font) { return 400; }
        @Override public void render(GuiGraphics g, Font f, int mx, int my, float dt) {}
        @Override public boolean mouseClicked(double mx,double my,int button) {
            if(button!=0)return false;
            clicks++;return true;
        }
    }
    private static class Fixture implements AutoCloseable {
        final Content content=new Content();
        final ScrollView scroll=new ScrollView(content);
        final UiRuntime runtime=new UiRuntime(new Font(null,false),new NativeWidgetHost() {
            public void add(AbstractWidget widget) {}
            public void remove(AbstractWidget widget) {}
        });
        Fixture() { runtime.setRoot(scroll);runtime.setViewport(0,0,100,100);runtime.flushFrameTasks(); }
        void flush() { runtime.flushFrameTasks();scroll.layoutTree(new Font(null,false),0,0,100,100); }
        public void close() {runtime.close();}
    }

    @Test void thumbPreservesGrabPositionAndCapturesOutsideTheViewport() {
        try(var f=new Fixture()) {
            assertTrue(f.runtime.mouseClicked(98,6,0));
            assertEquals(0,f.scroll.scrollOffset(),.001,"Grabbing a thumb must not jump");
            assertEquals(0,f.content.clicks,"The gutter must not activate content");
            assertTrue(f.runtime.mouseDragged(150,81,0,52,75));f.flush();
            assertEquals(300,f.scroll.scrollOffset(),.001);
            assertTrue(f.runtime.mouseDragged(150,-50,0,0,-131));f.flush();
            assertEquals(0,f.scroll.scrollOffset(),.001);
            assertTrue(f.runtime.mouseReleased(150,-50,0));
            f.runtime.mouseDragged(150,200,0,0,250);f.flush();
            assertEquals(0,f.scroll.scrollOffset(),.001);
        }
    }

    @Test void clickingTrackMovesTheThumbAndContentRemainsClickable() {
        try(var f=new Fixture()) {
            assertTrue(f.runtime.mouseClicked(98,60,0));f.flush();
            assertEquals(190,f.scroll.scrollOffset(),.001);
            assertTrue(f.runtime.mouseReleased(98,60,0));
            assertTrue(f.runtime.mouseClicked(20,20,0));
            assertEquals(1,f.content.clicks);
        }
    }

    @Test void rightButtonCannotStartOrReleaseLeftButtonDrag() {
        try(var f=new Fixture()) {
            assertFalse(f.runtime.mouseClicked(98,10,1));
            assertEquals(0,f.scroll.scrollOffset());
            assertTrue(f.runtime.mouseClicked(98,10,0));
            assertFalse(f.runtime.mouseReleased(98,10,1));
            assertTrue(f.runtime.mouseDragged(150,85,0,52,75));f.flush();
            assertEquals(300,f.scroll.scrollOffset());
            assertTrue(f.runtime.mouseReleased(150,85,0));
        }
    }

    @Test void escapeFreshPressAndDetachCancelDragging() {
        try(var f=new Fixture()) {
            assertTrue(f.runtime.mouseClicked(98,10,0));
            assertTrue(f.runtime.keyPressed(256,0,0));
            f.runtime.mouseDragged(98,85,0,0,75);f.flush();
            assertEquals(0,f.scroll.scrollOffset());
            assertTrue(f.runtime.mouseClicked(98,10,0));
            assertTrue(f.runtime.mouseClicked(20,20,0));
            f.runtime.mouseDragged(98,85,0,78,65);f.flush();
            assertEquals(0,f.scroll.scrollOffset());
            assertTrue(f.runtime.mouseClicked(98,10,0));
            f.runtime.setRoot(new Content());
            assertFalse(f.scroll.mouseDragged(150,85,0,52,75));
            assertEquals(0,f.scroll.scrollOffset());
        }
    }

    @Test void shrinkingContentCancelsDragAndWheelStillWorks() {
        try(var f=new Fixture()) {
            assertTrue(f.runtime.mouseScrolled(20,20,-1));f.flush();
            assertEquals(24,f.scroll.scrollOffset());
            assertTrue(f.runtime.mouseClicked(98,8,0));
            f.content.height(50);f.flush();
            assertEquals(0,f.scroll.scrollOffset());
            assertFalse(f.scroll.mouseDragged(150,85,0,52,77));
            assertFalse(f.runtime.mouseScrolled(20,20,-1));
        }
    }
    @Test void thumbPressPreservesSubpixelPositionInLongContent() {
        try (var f = new Fixture()) {
            f.content.height(10_000);
            f.scroll.scrollTo(48);
            f.flush();
            assertTrue(f.runtime.mouseClicked(98, 4, 0));
            assertEquals(48, f.scroll.scrollOffset(), .001, "A thumb press must not quantize scroll position");
            f.runtime.mouseDragged(98, 5, 0, 0, 1);
            assertEquals(48 + 9900.0 / 91, f.scroll.scrollOffset(), .001,
                    "A one-pixel move must retain the fractional grab position");
            f.runtime.mouseReleased(98, 5, 0);
        }
    }
}
