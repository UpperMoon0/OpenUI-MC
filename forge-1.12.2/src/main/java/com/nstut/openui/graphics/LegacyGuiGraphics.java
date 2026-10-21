package com.nstut.openui.graphics;
import com.nstut.openui.compat.LegacyFont;
import com.nstut.openui.compat.LegacyOrderedText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
/** Native immediate-mode 1.12 graphics, with transform-aware nested clipping. */
public final class LegacyGuiGraphics implements AutoCloseable {
    private final LegacyPoseStack pose;
    private final Deque<int[]> clips=new ArrayDeque<>();
    private final Consumer<int[]> scissor;
    private final boolean oldScissor;
    private final int[] oldBox;
    private boolean closed;
    public LegacyGuiGraphics() {
        pose=new LegacyPoseStack();scissor=this::applyClip;
        oldScissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);IntBuffer box=BufferUtils.createIntBuffer(4);GL11.glGetInteger(GL11.GL_SCISSOR_BOX,box);
        oldBox=new int[]{box.get(0),box.get(1),box.get(2),box.get(3)};
        GlStateManager.pushMatrix();
    }
    LegacyGuiGraphics(LegacyPoseStack pose,Consumer<int[]> scissor) { this.pose=pose;this.scissor=scissor;oldScissor=false;oldBox=null; }
    public LegacyPoseStack pose() { return pose; }
    public void flush() { }
    public int guiWidth() { return new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth(); }
    public int guiHeight() { return new ScaledResolution(Minecraft.getMinecraft()).getScaledHeight(); }
    public void fill(int x1,int y1,int x2,int y2,int color) { Gui.drawRect(x1,y1,x2,y2,color); }
    public int drawString(LegacyFont font,String text,int x,int y,int color,boolean shadow) { return font.nativeFont()==null?0:font.nativeFont().drawString(text,x,y,color,shadow); }
    public int drawString(LegacyFont font,ITextComponent text,int x,int y,int color,boolean shadow) { return drawString(font,text.getFormattedText(),x,y,color,shadow); }
    public int drawString(LegacyFont font,LegacyOrderedText text,int x,int y,int color,boolean shadow) { return drawString(font,text.text(),x,y,color,shadow); }
    public void blit(ResourceLocation texture,int x,int y,int u,int v,int width,int height) { blit(texture,x,y,width,height,u,v,width,height,256,256); }
    public void blit(ResourceLocation texture,int x,int y,int width,int height,float u,float v,int sourceW,int sourceH,int textureW,int textureH) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(x,y,u,v,sourceW,sourceH,width,height,textureW,textureH);
        GlStateManager.disableBlend();
    }
    public void renderItem(ItemStack stack,int x,int y) { Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack,x,y); }
    public void renderItemDecorations(LegacyFont font,ItemStack stack,int x,int y) { Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(font.nativeFont(),stack,x,y,null); }
    static int[] transformedClip(LegacyPoseStack pose,int x1,int y1,int x2,int y2) {
        double a=pose.x(x1),b=pose.x(x2),c=pose.y(y1),d=pose.y(y2);
        return new int[]{(int)Math.floor(Math.min(a,b)),(int)Math.floor(Math.min(c,d)),(int)Math.ceil(Math.max(a,b)),(int)Math.ceil(Math.max(c,d))};
    }
    public void enableScissor(int x1,int y1,int x2,int y2) {
        int[] r=transformedClip(pose,x1,y1,x2,y2);
        if(!clips.isEmpty()) { int[] p=clips.peek();r[0]=Math.max(r[0],p[0]);r[1]=Math.max(r[1],p[1]);r[2]=Math.min(r[2],p[2]);r[3]=Math.min(r[3],p[3]); }
        r[2]=Math.max(r[0],r[2]);r[3]=Math.max(r[1],r[3]);clips.push(r);scissor.accept(r);
    }
    public void disableScissor() { if(!clips.isEmpty())clips.pop();scissor.accept(clips.peek()); }
    static int[] intersectFramebufferClip(int[] clip,int[] outer) {
        int x=Math.max(clip[0],outer[0]),y=Math.max(clip[1],outer[1]);
        int right=Math.min(clip[0]+clip[2],outer[0]+outer[2]),top=Math.min(clip[1]+clip[3],outer[1]+outer[3]);
        return new int[]{x,y,Math.max(0,right-x),Math.max(0,top-y)};
    }
    private void restoreScissor() {
        GL11.glScissor(oldBox[0],oldBox[1],oldBox[2],oldBox[3]);
        if(oldScissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    private void applyClip(int[] r) {
        if(r==null) { restoreScissor();return; }
        Minecraft mc=Minecraft.getMinecraft();int scale=new ScaledResolution(mc).getScaleFactor();
        int[] box={r[0]*scale,mc.displayHeight-r[3]*scale,Math.max(0,r[2]-r[0])*scale,Math.max(0,r[3]-r[1])*scale};
        if(oldScissor)box=intersectFramebufferClip(box,oldBox);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor(box[0],box[1],box[2],box[3]);
    }
    public void close() {
        if(closed)return;closed=true;
        try { pose.unwind();clips.clear(); }
        finally {
            if(oldBox==null)scissor.accept(null);
            else { GlStateManager.popMatrix();restoreScissor(); }
        }
    }
}
