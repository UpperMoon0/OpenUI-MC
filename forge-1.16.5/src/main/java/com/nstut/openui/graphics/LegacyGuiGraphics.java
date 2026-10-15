package com.nstut.openui.graphics;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;
import java.util.ArrayDeque;
import java.util.Deque;
/** Version adapter for OpenUI drawing and nested GUI-space clipping on Minecraft 1.16.5. */
public final class LegacyGuiGraphics {
    private final MatrixStack pose;
    private final Deque<int[]> clips=new ArrayDeque<>();
    public LegacyGuiGraphics(MatrixStack pose) { this.pose=pose; }
    public void flush() { } // Legacy primitives submit immediately.
    public MatrixStack pose() { return pose; }
    public int guiWidth() { return Minecraft.getInstance().getWindow().getGuiScaledWidth(); }
    public int guiHeight() { return Minecraft.getInstance().getWindow().getGuiScaledHeight(); }
    public void fill(int x1,int y1,int x2,int y2,int color) { AbstractGui.fill(pose,x1,y1,x2,y2,color); }
    public int drawString(FontRenderer font,String text,int x,int y,int color,boolean shadow) { return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color); }
    public int drawString(FontRenderer font,ITextComponent text,int x,int y,int color,boolean shadow) { return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color); }
    public int drawString(FontRenderer font,IReorderingProcessor text,int x,int y,int color,boolean shadow) { return shadow?font.drawShadow(pose,text,x,y,color):font.draw(pose,text,x,y,color); }
    public void blit(ResourceLocation texture,int x,int y,int u,int v,int width,int height) { blit(texture,x,y,width,height,u,v,width,height,256,256); }
    public void blit(ResourceLocation texture,int x,int y,int width,int height,float u,float v,int sourceW,int sourceH,int textureW,int textureH) {
        Minecraft.getInstance().getTextureManager().bind(texture); RenderSystem.enableBlend();
        AbstractGui.blit(pose,x,y,width,height,u,v,sourceW,sourceH,textureW,textureH);
        RenderSystem.disableBlend();
    }
    public void renderItem(ItemStack stack,int x,int y) { Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack,x,y); }
    public void renderItemDecorations(FontRenderer font,ItemStack stack,int x,int y) { Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font,stack,x,y); }
    public void enableScissor(int x1,int y1,int x2,int y2) {
        if(!clips.isEmpty()) { int[] parent=clips.peek(); x1=Math.max(x1,parent[0]);y1=Math.max(y1,parent[1]);x2=Math.min(x2,parent[2]);y2=Math.min(y2,parent[3]); }
        clips.push(new int[]{x1,y1,Math.max(x1,x2),Math.max(y1,y2)}); applyClip();
    }
    public void disableScissor() { if(!clips.isEmpty()) clips.pop(); if(clips.isEmpty()) GL11.glDisable(GL11.GL_SCISSOR_TEST); else applyClip(); }
    private void applyClip() {
        int[] r=clips.peek(); double scale=Minecraft.getInstance().getWindow().getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor((int)Math.floor(r[0]*scale),(int)Math.floor(Minecraft.getInstance().getWindow().getHeight()-r[3]*scale),(int)Math.ceil((r[2]-r[0])*scale),(int)Math.ceil((r[3]-r[1])*scale));
    }
}
