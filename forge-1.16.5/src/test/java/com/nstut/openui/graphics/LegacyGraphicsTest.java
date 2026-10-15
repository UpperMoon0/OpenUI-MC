package com.nstut.openui.graphics;
import com.mojang.blaze3d.matrix.MatrixStack;
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
}
