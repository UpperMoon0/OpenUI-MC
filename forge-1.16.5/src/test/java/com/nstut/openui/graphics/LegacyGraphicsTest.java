package com.nstut.openui.graphics;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.nstut.openui.api.ClipStack;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LegacyGraphicsTest {
    @Test void clippingUsesTheSamePoseAsAnimatedContent() {
        MatrixStack pose=new MatrixStack(); pose.translate(10,20,0); pose.scale(2,3,1);
        assertArrayEquals(new int[]{12,26,20,38},LegacyGuiGraphics.transformedClip(pose,1,2,5,6));
        pose.pushPose(); pose.translate(2,4,0);
        assertArrayEquals(new int[]{16,38,24,50},LegacyGuiGraphics.transformedClip(pose,1,2,5,6));
        pose.popPose(); assertArrayEquals(new int[]{12,26,20,38},LegacyGuiGraphics.transformedClip(pose,1,2,5,6));
    }
    @Test void generatedClipStackIntersectsOnlyAfterTransformingEachLocalRectangle() {
        MatrixStack pose=new MatrixStack();
        AtomicReference<int[]> applied=new AtomicReference<>();
        LegacyGuiGraphics graphics=new LegacyGuiGraphics(pose,applied::set);
        ClipStack.push(graphics,0,0,100,100);
        assertArrayEquals(new int[]{0,0,100,100},applied.get());
        pose.pushPose(); pose.translate(-90,0,0);
        ClipStack.push(graphics,100,0,100,100);
        assertArrayEquals(new int[]{10,0,100,100},applied.get());
        pose.pushPose(); pose.scale(2,1,1);
        ClipStack.push(graphics,60,20,30,50);
        assertArrayEquals(new int[]{30,20,90,70},applied.get());
        // Restore the saved screen-space parent even while the child pose is active.
        ClipStack.pop(graphics);
        assertArrayEquals(new int[]{10,0,100,100},applied.get());
        pose.popPose();
        ClipStack.push(graphics,300,0,20,20);
        assertEquals(applied.get()[0],applied.get()[2]);
        ClipStack.pop(graphics);
        assertArrayEquals(new int[]{10,0,100,100},applied.get());
        ClipStack.pop(graphics);
        assertArrayEquals(new int[]{0,0,100,100},applied.get());
        pose.popPose(); ClipStack.pop(graphics);
        assertNull(applied.get());
        // A completed frame must not leave a stale parent or stack depth behind.
        ClipStack.push(graphics,200,200,20,20);
        assertArrayEquals(new int[]{200,200,220,220},applied.get());
        ClipStack.pop(graphics); assertNull(applied.get());
    }
}
