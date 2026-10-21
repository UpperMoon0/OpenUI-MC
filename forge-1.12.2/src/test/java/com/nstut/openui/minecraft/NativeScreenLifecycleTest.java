package com.nstut.openui.minecraft;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.compat.LegacyFont;
import com.nstut.openui.graphics.LegacyGuiGraphics;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.util.text.TextComponentString;
import org.junit.Test;
import static org.junit.Assert.*;
/** Exercises the actual GuiScreen adapter without a display or replacing game classes. */
public class NativeScreenLifecycleTest {
    private static final class Probe extends UIComponent {
        int mounts,unmounts;
        @Override public int preferredWidth(LegacyFont font) { return 10; }
        @Override public int preferredHeight(LegacyFont font) { return 10; }
        @Override public void render(LegacyGuiGraphics graphics,LegacyFont font,int x,int y,float tick) { }
        @Override protected void onMount() { mounts++; }
        @Override protected void onUnmount() { unmounts++; }
    }
    private static final class Screen extends UiScreen {
        Probe root;
        Screen() { super(new TextComponentString("lifecycle test")); }
        @Override protected UIComponent buildUI() { root=new Probe();return root; }
        void rebuild() { rebuildUI(); }
    }
    @Test public void repeatedNativeInitializationDisposesThePreviousRuntimeExactlyOnce() {
        Screen screen=new Screen();screen.width=100;screen.height=60;screen.initGui();
        Probe first=screen.root;UiRuntime previous=screen.uiRuntime();assertEquals(1,first.mounts);
        screen.width=200;screen.height=120;screen.initGui();
        assertNotSame(previous,screen.uiRuntime());assertEquals(1,first.unmounts);assertEquals(1,screen.root.mounts);
        previous.close();assertEquals(1,first.unmounts);
        Probe second=screen.root;screen.onGuiClosed();screen.onGuiClosed();
        assertEquals(1,second.unmounts);assertNull(screen.uiRuntime());
    }
    @Test public void rebuildingUnmountsTheOldTreeWithoutReplacingTheRuntime() {
        Screen screen=new Screen();screen.initGui();Probe first=screen.root;UiRuntime runtime=screen.uiRuntime();
        screen.rebuild();assertSame(runtime,screen.uiRuntime());assertEquals(1,first.unmounts);assertEquals(1,screen.root.mounts);
        Probe current=screen.root;screen.onGuiClosed();assertEquals(1,current.unmounts);
    }
}
