package com.nstut.openui.compat;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextComponentString;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import net.minecraft.client.Minecraft;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
public class LegacyTextFieldTest {
    private Object previousMinecraft;
    private Field instance;
    @Before public void headlessClientOwner() throws Exception {
        Field singleton=Unsafe.class.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        Unsafe unsafe=(Unsafe)singleton.get(null);
        instance=Minecraft.class.getDeclaredField("instance");instance.setAccessible(true);
        previousMinecraft=instance.get(null);instance.set(null,unsafe.allocateInstance(Minecraft.class));
    }
    @After public void restoreClientOwner() throws Exception { if(instance!=null)instance.set(null,previousMinecraft); }
    private static final class TestFont extends FontRenderer {
        private TestFont() { super(null,null,null,false); }
        @Override public int getStringWidth(String text) { return text.length()*6; }
        @Override public String trimStringToWidth(String text,int width) { return text.substring(0,Math.min(text.length(),Math.max(0,width/6))); }
        @Override public String trimStringToWidth(String text,int width,boolean reverse) { int count=Math.min(text.length(),Math.max(0,width/6));return reverse?text.substring(text.length()-count):text.substring(0,count); }
    }
    private LegacyTextField field() throws Exception {
        Field singleton=Unsafe.class.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        TestFont font=(TestFont)((Unsafe)singleton.get(null)).allocateInstance(TestFont.class);font.FONT_HEIGHT=9;
        return new LegacyTextField(new LegacyFont(font),0,0,100,18,new TextComponentString(""));
    }
    @Test public void resizeRetainsTextSelectionFocusAndResponderWithoutExtraCommit() throws Exception {
        LegacyTextField field=field();AtomicInteger changes=new AtomicInteger();field.setResponder(value->changes.incrementAndGet());
        field.setValue("abcd");assertEquals(1,changes.get());field.setCursorPosition(1);field.setHighlightPos(3);field.setFocus(true);
        field.setWidth(60);field.y=20;
        assertTrue(field.charTyped('X',0));
        assertEquals("aXd",field.getValue());assertTrue(field.isFocused());assertEquals(2,changes.get());
        field.setValue("aXd");assertEquals(2,changes.get());
    }
    @Test public void maxLengthAndDisabledInvisibleFieldsRemainAuthoritative() throws Exception {
        LegacyTextField field=field();field.setMaxLength(3);field.setValue("abcdef");assertEquals("abc",field.getValue());field.setFocus(true);
        field.active=false;assertFalse(field.charTyped('X',0));field.active=true;field.visible=false;assertFalse(field.charTyped('X',0));
        field.visible=true;field.setValue("");assertTrue(field.charTyped('X',0));assertEquals("X",field.getValue());
    }
}
