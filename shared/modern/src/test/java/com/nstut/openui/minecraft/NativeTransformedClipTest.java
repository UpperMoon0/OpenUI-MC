package com.nstut.openui.minecraft;
import com.nstut.openui.api.ClipStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fStack;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import static org.junit.jupiter.api.Assertions.*;

/** Uses genuine native transforms/intersection; no recording-scissor substitute. */
class NativeTransformedClipTest {
    private static GuiGraphicsExtractor graphics() throws Exception {
        Class<?> unsafe=Class.forName("sun.misc.Unsafe");Field singleton=unsafe.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        GuiGraphicsExtractor graphics=(GuiGraphicsExtractor)unsafe.getMethod("allocateInstance",Class.class).invoke(singleton.get(null),GuiGraphicsExtractor.class);
        Field pose=GuiGraphicsExtractor.class.getDeclaredField("pose");pose.setAccessible(true);pose.set(graphics,new Matrix3x2fStack(16));
        Class<?> type=Class.forName("net.minecraft.client.gui.GuiGraphicsExtractor$ScissorStack");
        Object stack;
        try { Constructor<?> ctor=type.getDeclaredConstructor(ScreenRectangle.class);ctor.setAccessible(true);stack=ctor.newInstance(new ScreenRectangle(0,0,1000,1000)); }
        catch(NoSuchMethodException olderNativeApi) { Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);stack=ctor.newInstance(); }
        Field scissors=GuiGraphicsExtractor.class.getDeclaredField("scissorStack");scissors.setAccessible(true);scissors.set(graphics,stack);return graphics;
    }
    private static Object active(GuiGraphicsExtractor graphics) throws Exception {
        Field field=GuiGraphicsExtractor.class.getDeclaredField("scissorStack");field.setAccessible(true);Object stack=field.get(graphics);
        Method peek=stack.getClass().getDeclaredMethod("peek");peek.setAccessible(true);return peek.invoke(stack);
    }
    @Test void nestedDifferentPosesIntersectAfterNativeTransformation() throws Exception {
        GuiGraphicsExtractor direct=graphics(), actual=graphics();
        direct.enableScissor(0,0,100,100);direct.pose().translate(-90,0);direct.enableScissor(100,0,200,100);
        ClipStack.push(actual,0,0,100,100);actual.pose().translate(-90,0);ClipStack.push(actual,100,0,100,100);
        assertEquals(new ScreenRectangle(10,0,90,100),active(direct));assertEquals(active(direct),active(actual));
        ClipStack.pop(actual);assertEquals(new ScreenRectangle(0,0,100,100),active(actual));ClipStack.pop(actual);
    }
    @Test void incomingNativeClipAndDifferentExtractorOwnershipArePreserved() throws Exception {
        GuiGraphicsExtractor first=graphics(),second=graphics();first.enableScissor(10,20,80,90);Object incoming=active(first);
        ClipStack.push(first,0,0,50,50);ClipStack.push(second,1,2,30,40);
        ClipStack.pop(first);assertEquals(incoming,active(first));
        assertEquals(new ScreenRectangle(1,2,30,40),active(second));
        ClipStack.pop(first);assertEquals(incoming,active(first),"Unmatched pop cannot remove an external clip");ClipStack.pop(second);
    }
}
