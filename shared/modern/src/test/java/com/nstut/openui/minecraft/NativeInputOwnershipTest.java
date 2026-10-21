package com.nstut.openui.minecraft;

import com.nstut.openui.api.*;
import com.nstut.openui.input.*;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.runtime.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Real native adapters, runtime, keyboard events and EditBox insertion. No display is needed. */
class NativeInputOwnershipTest {
    private static Font font() { return new Font(null) {
        @Override public String plainSubstrByWidth(String text,int width) { return text; }
        @Override public int width(String text) { return text.length()*6; }
    }; }
    private static EditBoxWrapper field() {
        EditBox nativeBox=new EditBox(font(),0,0,100,16,Component.empty()) {
            private boolean focus;
            // Only the game-singleton focus side effect is substituted.
            @Override public void setFocused(boolean value) { focus=value; }
            @Override public boolean isFocused() { return focus; }
        };
        nativeBox.setMaxLength(64);return new EditBoxWrapper(nativeBox);
    }
    private static UiRuntime runtime(UIComponent root) {
        UiRuntime runtime=new UiRuntime(font(),new NativeWidgetHost() {
            public void add(AbstractWidget widget) { }
            public void remove(AbstractWidget widget) { }
        });
        runtime.setRoot(root);root.requestFocus();return runtime;
    }
    private static class Screen extends UiScreen {
        Screen() { super(Component.empty()); }
        protected UIComponent buildUI() { return null; }
    }
    private static class Container extends UiContainerScreen<AbstractContainerMenu> {
        Container() { super(null,null,Component.empty()); }
        protected UIComponent buildUI() { return null; }
        @Override public int getImageWidth() { return 219; }
        @Override public int getImageHeight() { return 143; }
    }
    @SuppressWarnings("unchecked")
    private static <T> T adapter(Class<T> type,Class<?> owner,UiRuntime runtime) throws Exception {
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        T value=(T)unsafeType.getMethod("allocateInstance",Class.class).invoke(singleton.get(null),type);
        Field field=owner.getDeclaredField("uiRuntime");field.setAccessible(true);field.set(value,runtime);return value;
    }
    @Test void bothNativeAdaptersPreserveBmpAndSupplementaryCharacters() throws Exception {
        EditBoxWrapper text=field();try(UiRuntime runtime=runtime(text)) {
            Screen screen=adapter(Screen.class,UiScreen.class,runtime);
            Container container=adapter(Container.class,UiContainerScreen.class,runtime);
            assertTrue(screen.charTyped(new CharacterEvent('A')));
            assertTrue(container.charTyped(new CharacterEvent(0x1F600)));
            assertEquals("A\uD83D\uDE00",text.getValue());
            assertEquals(219,container.containerWidth());assertEquals(143,container.containerHeight());
        }
    }
    @Test void cancellableEventContainsOneFullCodePointAndNeverInsertsIt() throws Exception {
        EditBoxWrapper text=field();AtomicInteger events=new AtomicInteger();
        text.on(EventType.CHAR_TYPED,event->{
            KeyboardEvent typed=(KeyboardEvent)event;assertEquals(0x1F600,typed.codePoint());
            assertEquals('\0',typed.character());events.incrementAndGet();event.preventDefault();
        });
        try(UiRuntime runtime=runtime(text)) {
            assertTrue(adapter(Screen.class,UiScreen.class,runtime).charTyped(new CharacterEvent(0x1F600)));
            assertEquals(1,events.get());assertEquals("",text.getValue());
        }
    }
    @Test void textAndNonDismissableModalNeverReachNativeContainerHotkeys() throws Exception {
        EditBoxWrapper text=field();try(UiRuntime runtime=runtime(text)) {
            Container container=adapter(Container.class,UiContainerScreen.class,runtime);
            // No native Minecraft/menu fields were initialized. Any vanilla
            // THROW/SWAP fallback would fail, instead of silently satisfying this test.
            assertTrue(container.keyPressed(new KeyEvent(81,0,0)));
            assertTrue(container.keyPressed(new KeyEvent(49,0,0)));
            runtime.overlays().show(OverlayLayer.MODAL,Ui.text("blocking"),true,false,false,null);
            assertTrue(container.keyPressed(new KeyEvent(81,0,0)));
            assertTrue(container.keyPressed(new KeyEvent(49,0,0)));
            assertTrue(container.keyPressed(new KeyEvent(256,0,0)));
            assertTrue(runtime.overlays().hasBlockingOverlay());
            assertFalse(runtime.charTyped(0x1F600,0));assertEquals("",text.getValue());
            assertTrue(adapter(Screen.class,UiScreen.class,runtime).keyPressed(new KeyEvent(256,0,0)));
        }
    }
    @Test void oldCharOverridesRemainCompatibleWithoutTruncatingSupplementaryInput() {
        AtomicInteger seen=new AtomicInteger();UIComponent legacy=new Stack() {
            @Override public boolean charTyped(char character,int modifiers) { seen.set(character);return true; }
        }.focusable(true);
        try(UiRuntime runtime=runtime(legacy)) {
            assertTrue(runtime.charTyped('x',0));assertEquals('x',seen.get());
            assertFalse(runtime.charTyped(0x1F600,0));assertEquals('x',seen.get());
            assertFalse(runtime.charTyped(0xD800,0));assertFalse(runtime.charTyped(0x110000,0));
        }
    }
    @Test void existingTextFieldCharOverridesFilterBmpExactlyOnce() throws Exception {
        AtomicInteger seen=new AtomicInteger();
        EditBoxWrapper text=new EditBoxWrapper(field().getEditBox()) {
            @Override public boolean charTyped(char character,int modifiers) {
                seen.incrementAndGet();return character=='x' || super.charTyped(character,modifiers);
            }
        };
        try(UiRuntime runtime=runtime(text)) {
            Screen screen=adapter(Screen.class,UiScreen.class,runtime);
            assertTrue(screen.charTyped(new CharacterEvent('x')));
            assertEquals(1,seen.get());assertEquals("",text.getValue());
            assertTrue(screen.charTyped(new CharacterEvent('a')));
            assertEquals(2,seen.get());assertEquals("a",text.getValue());
            assertTrue(screen.charTyped(new CharacterEvent(0x1F600)));
            assertEquals(2,seen.get());assertEquals("a\uD83D\uDE00",text.getValue());
        }
        AtomicInteger textFieldCalls=new AtomicInteger();
        com.nstut.openui.controls.TextField typed=new com.nstut.openui.controls.TextField(
                com.nstut.openui.state.Signals.of(""),font()) {
            @Override public boolean charTyped(char character,int modifiers) { textFieldCalls.incrementAndGet();return true; }
        };
        assertTrue(typed.charTyped((int)'x',0));assertEquals(1,textFieldCalls.get());
        assertEquals("",typed.getValue());
    }
    @Test void existingPaletteCharOverridesFilterBmpExactlyOnce() throws Exception {
        AtomicInteger seen=new AtomicInteger();
        com.nstut.openui.controls.CommandPalette palette=new com.nstut.openui.controls.CommandPalette() {
            @Override public boolean charTyped(char character,int modifiers) {
                seen.incrementAndGet();return character=='x' || super.charTyped(character,modifiers);
            }
        };
        assertTrue(palette.charTyped((int)'x',0));assertTrue(palette.charTyped((int)'a',0));
        assertTrue(palette.charTyped(0x1F600,0));assertEquals(2,seen.get());
        Field query=com.nstut.openui.controls.CommandPalette.class.getDeclaredField("query");query.setAccessible(true);
        assertEquals("a\uD83D\uDE00",query.get(palette));
    }
    @Test void commandPaletteDeletesASupplementaryCharacterAsOneCodePoint() throws Exception {
        com.nstut.openui.controls.CommandPalette palette=new com.nstut.openui.controls.CommandPalette();
        assertTrue(palette.charTyped(0x1F600,0));
        Field query=palette.getClass().getDeclaredField("query");query.setAccessible(true);
        assertEquals("\uD83D\uDE00",query.get(palette));
        assertTrue(palette.keyPressed(259,0,0));assertEquals("",query.get(palette));
    }
    @Test void commonPolicyAllowsNormalEscapeButOwnsTabAndModalEscape() {
        AtomicInteger nativeCalls=new AtomicInteger();
        assertTrue(ContainerKeyPolicy.dispatch(256,false,true,false,()->false,()->{nativeCalls.incrementAndGet();return true;}));
        assertEquals(1,nativeCalls.get());
        for(int key:new int[]{81,49,258})assertTrue(ContainerKeyPolicy.dispatch(key,false,true,false,()->false,()->{fail("text leaked to vanilla");return false;}));
        assertTrue(ContainerKeyPolicy.dispatch(256,false,false,true,()->false,()->{fail("modal Escape leaked");return false;}));
    }
}
