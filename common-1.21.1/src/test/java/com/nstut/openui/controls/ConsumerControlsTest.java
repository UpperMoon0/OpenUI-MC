package com.nstut.openui.controls;
import com.nstut.openui.api.*;
import com.nstut.openui.state.Signals;
import com.nstut.openui.runtime.*;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class ConsumerControlsTest {
    private static Font font() { return new Font(null, false) {
        @Override public String plainSubstrByWidth(String value, int width) { return value.substring(0,Math.min(value.length(),Math.max(0,width/6))); }
        @Override public int width(String value) { return value.length()*6; }
    }; }

    @Test void enterRetriesButBlurDoesNotDoubleSubmit() {
        TextField field = new TextField(Signals.of("1"), font());
        AtomicInteger commits = new AtomicInteger();
        field.onCommit(value -> commits.incrementAndGet());
        field.onFocusLost(); assertEquals(0, commits.get());
        field.setValue("2"); field.onFocusLost(); assertEquals(1, commits.get());
        field.onFocusLost(); assertEquals(1, commits.get());
        assertTrue(field.keyPressed(257,0,0)); assertEquals(2, commits.get());
        field.onFocusLost(); assertEquals(2, commits.get());
        assertTrue(field.keyPressed(335,0,0)); assertEquals(3, commits.get());
    }
    @Test void signalDraftChangedBeforeMountIsAppliedOnMount() {
        com.nstut.openui.state.Signal<String> draft=Signals.of("3");
        TextField field=new TextField(draft,font());
        draft.set("-");
        UiRuntime runtime=new UiRuntime(font(),new NativeWidgetHost() { public void add(net.minecraft.client.gui.components.AbstractWidget w) {} public void remove(net.minecraft.client.gui.components.AbstractWidget w) {} });
        runtime.setRoot(field); assertEquals("-",field.getValue());
        runtime.close(); draft.set("4");
        UiRuntime reopened=new UiRuntime(font(),new NativeWidgetHost() { public void add(net.minecraft.client.gui.components.AbstractWidget w) {} public void remove(net.minecraft.client.gui.components.AbstractWidget w) {} });
        reopened.setRoot(field); assertEquals("4",field.getValue()); reopened.close();
    }
    @Test void iconContentRetainsButtonHitAndFocus() {
        AtomicInteger pressed = new AtomicInteger();
        ButtonWidget button = new ButtonWidget("Array").content(IconWidget.texture("test","array.png",256,256).size(16)).onPress(pressed::incrementAndGet);
        Radial radial = new Radial(40,24).item(button);
        UiRuntime runtime = new UiRuntime(font(), new NativeWidgetHost() { public void add(net.minecraft.client.gui.components.AbstractWidget w) {} public void remove(net.minecraft.client.gui.components.AbstractWidget w) {} });
        runtime.setRoot(radial); runtime.setViewport(0,0,120,120);
        runtime.mouseClicked(60,20,0); runtime.mouseReleased(60,20,0);
        assertEquals(1, pressed.get()); assertSame(button,runtime.focus().focused());
        assertEquals(24, button.preferredWidth(null));
        UIComponent old = button.children().get(0);
        button.content(IconWidget.of("+",0xFFFFFFFF));
        assertNull(old.parent()); assertEquals(1,button.children().size());
        runtime.close();
    }
    @Test void radialRemovalAndReparentingReleaseLayoutOwnership() {
        UIComponent first=new Panel(), second=new Panel(), center=new Panel();
        Radial radial=new Radial(40,24).item(first).item(second).center(center,30);
        radial.layout(0,0,120,120);
        assertTrue(radial.removeChild(first));
        first.layout(200,200,10,10);
        radial.layout(0,0,120,120);
        assertEquals(200,first.getX());
        assertEquals(8,second.getY()); // The remaining item moves to the top.
        Panel newParent=new Panel();
        newParent.addChild(second); newParent.addChild(center);
        second.layout(300,300,10,10); center.layout(400,400,10,10);
        radial.layout(0,0,120,120);
        assertEquals(300,second.getX()); assertEquals(400,center.getX());
        radial.item(first); radial.clearChildren(); first.layout(500,500,10,10);
        radial.layout(0,0,120,120); assertEquals(500,first.getX());
    }
    @Test void compositeButtonReleasesDetachedContent() {
        UIComponent icon=IconWidget.of("+",0xFFFFFFFF);
        ButtonWidget button=new ButtonWidget("Label").content(icon);
        Panel other=new Panel(); other.addChild(icon);
        icon.layout(200,200,16,16); button.layout(0,0,40,24);
        assertEquals(200,icon.getX()); assertEquals(60,button.preferredWidth(null));
        button.content(icon); button.clearChildren(); icon.layout(300,300,16,16);
        button.layout(0,0,40,24); assertEquals(300,icon.getX());
    }
    @Test void replacementTransfersRadialAndButtonRoles() {
        UIComponent oldItem=new Panel(), item=new Panel(), oldCenter=new Panel(), center=new Panel();
        Radial radial=new Radial(40,24).item(oldItem).center(oldCenter,30);
        radial.replaceChild(0,item); radial.replaceChild(1,center);
        oldItem.layout(200,200,10,10); oldCenter.layout(300,300,10,10);
        radial.layout(0,0,120,120);
        assertEquals(8,item.getY()); assertEquals(45,center.getY());
        assertEquals(200,oldItem.getY()); assertEquals(300,oldCenter.getY());
        UIComponent oldIcon=IconWidget.of("+",0xFFFFFFFF), icon=IconWidget.of("-",0xFFFFFFFF);
        ButtonWidget button=new ButtonWidget("Label").content(oldIcon);
        button.replaceChild(0,icon); oldIcon.layout(400,400,16,16);
        button.layout(0,0,40,24);
        assertEquals(4,icon.getX()); assertEquals(400,oldIcon.getX());
    }
    @Test void removalReleasesContentBeforeUnmountCallbacks() {
        ButtonWidget button=new ButtonWidget("Label");
        AtomicInteger callbacks=new AtomicInteger();
        UIComponent content=new Panel() {
            @Override protected void onUnmount() {
                layout(200,200,10,10);
                button.layout(0,0,40,24);
                assertEquals(200,getX());
                assertEquals(60,button.preferredWidth(null));
                callbacks.incrementAndGet();
            }
        };
        button.content(content);
        UiRuntime runtime=new UiRuntime(font(),new NativeWidgetHost() { public void add(net.minecraft.client.gui.components.AbstractWidget w) {} public void remove(net.minecraft.client.gui.components.AbstractWidget w) {} });
        runtime.setRoot(button); button.removeChild(content);
        assertEquals(1,callbacks.get()); runtime.close();
    }
    @Test void expandedDescendantCanBeRevealedWithoutFocus() {
        UIComponent before=new Panel().height(200), target=new Panel().height(24);
        ScrollView scroll=new ScrollView(Ui.column(before,target));
        scroll.layout(0,0,100,80); scroll.reveal(target); scroll.layout(0,0,100,80);
        assertTrue(target.getY()>=0); assertTrue(target.getY()+target.getHeight()<=80);
        assertFalse(target.isFocused());
        assertThrows(IllegalArgumentException.class,()->scroll.reveal(new Panel()));
    }
    @Test void scrollRestoresAndClampsAfterResize() {
        UIComponent tall = new UIComponent() { @Override public int preferredWidth(Font f) { return 100; } @Override public int preferredHeight(Font font) { return 400; } @Override public void render(net.minecraft.client.gui.GuiGraphics g, Font f, int x,int y,float dt) {} };
        ScrollView scroll = new ScrollView(tall).scrollTo(150);
        scroll.layout(0,0,100,100); assertEquals(150,scroll.scrollOffset()); assertEquals(-150,tall.getY());
        scroll.layout(0,0,100,350); assertEquals(50,scroll.scrollOffset());
        scroll.scrollTo(-10); scroll.layout(0,0,100,100); assertEquals(0,scroll.scrollOffset());
        assertThrows(IllegalArgumentException.class,()->scroll.scrollTo(Double.NaN));
    }
}
