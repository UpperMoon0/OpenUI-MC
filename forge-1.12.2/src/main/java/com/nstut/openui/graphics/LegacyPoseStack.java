package com.nstut.openui.graphics;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.renderer.GlStateManager;
/** Tracks GUI transforms for clipping, applying the same transforms to native 1.12 drawing. */
public final class LegacyPoseStack {
    private final Deque<double[]> stack=new ArrayDeque<>();
    private final boolean nativeDrawing;
    private double[] pose={1,1,0,0};
    public LegacyPoseStack() { this(true); }
    LegacyPoseStack(boolean nativeDrawing) { this.nativeDrawing=nativeDrawing; }
    public void pushPose() { stack.push(pose.clone());if(nativeDrawing)GlStateManager.pushMatrix(); }
    public void popPose() { if(stack.isEmpty())throw new IllegalStateException("Unbalanced GUI transform");pose=stack.pop();if(nativeDrawing)GlStateManager.popMatrix(); }
    public void translate(double x,double y,double z) { pose[2]+=pose[0]*x;pose[3]+=pose[1]*y;if(nativeDrawing)GlStateManager.translate(x,y,z); }
    public void scale(float x,float y,float z) { pose[0]*=x;pose[1]*=y;if(nativeDrawing)GlStateManager.scale(x,y,z); }
    void unwind() { while(!stack.isEmpty())popPose();pose=new double[]{1,1,0,0}; }
    double x(double x) { return pose[0]*x+pose[2]; }
    double y(double y) { return pose[1]*y+pose[3]; }
}
