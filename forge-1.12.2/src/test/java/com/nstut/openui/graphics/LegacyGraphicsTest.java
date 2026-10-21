package com.nstut.openui.graphics;
import com.nstut.openui.api.ClipStack;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;
public class LegacyGraphicsTest {
    @Test public void clippingUsesTheSamePoseAsAnimatedContent() {
        LegacyPoseStack pose=new LegacyPoseStack(false); pose.translate(10,20,0); pose.scale(2,3,1);
        assertArrayEquals(new int[]{12,26,20,38},LegacyGuiGraphics.transformedClip(pose,1,2,5,6));
        pose.pushPose(); pose.translate(2,4,0);
        assertArrayEquals(new int[]{16,38,24,50},LegacyGuiGraphics.transformedClip(pose,1,2,5,6));
        pose.popPose(); assertArrayEquals(new int[]{12,26,20,38},LegacyGuiGraphics.transformedClip(pose,1,2,5,6));
    }
    @Test public void generatedClipStackIntersectsOnlyAfterTransformingEachLocalRectangle() {
        LegacyPoseStack pose=new LegacyPoseStack(false);
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
    @Test public void failedFrameCleanupUnwindsTransformsAndClearsNativeClip() {
        LegacyPoseStack pose=new LegacyPoseStack(false);java.util.concurrent.atomic.AtomicReference<int[]> clip=new java.util.concurrent.atomic.AtomicReference<>();
        LegacyGuiGraphics graphics=new LegacyGuiGraphics(pose,clip::set);pose.pushPose();pose.translate(10,20,0);graphics.enableScissor(0,0,10,10);
        graphics.close();assertNull(clip.get());assertArrayEquals(new int[]{0,0,10,10},LegacyGuiGraphics.transformedClip(pose,0,0,10,10));
        assertThrows(IllegalStateException.class,pose::popPose);
    }
    @Test public void externalFramebufferClipRemainsAnOuterBoundary() {
        assertArrayEquals(new int[]{10,20,40,30},LegacyGuiGraphics.intersectFramebufferClip(new int[]{0,0,100,100},new int[]{10,20,40,30}));
        assertArrayEquals(new int[]{90,90,0,0},LegacyGuiGraphics.intersectFramebufferClip(new int[]{90,90,10,10},new int[]{10,20,40,30}));
    }
    @Test public void closingTwiceDoesNotPopAnotherOwnersMatrixAndRootTranslationIsReset() {
        LegacyPoseStack pose=new LegacyPoseStack(false);java.util.concurrent.atomic.AtomicInteger closes=new java.util.concurrent.atomic.AtomicInteger();
        LegacyGuiGraphics graphics=new LegacyGuiGraphics(pose,clip->closes.incrementAndGet());pose.translate(10,20,0);
        graphics.close();graphics.close();assertEquals(1,closes.get());
        assertArrayEquals(new int[]{0,0,10,10},LegacyGuiGraphics.transformedClip(pose,0,0,10,10));
    }
}
