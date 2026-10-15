package com.nstut.openui.api;
import com.nstut.openui.layout.RadialLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.util.ArrayList;
import java.util.List;
/** Radial arrangement of ordinary OpenUI controls, retaining their normal focus and input semantics. */
public final class Radial extends UIComponent {
    private final List<UIComponent> items=new ArrayList<>();
    private int radius,buttonSize,centerSize;
    private UIComponent center;
    public Radial(int radius,int buttonSize) { dimensions(radius,buttonSize); }
    public Radial dimensions(int radius,int buttonSize) { if(radius<0 || buttonSize<=0) throw new IllegalArgumentException("Invalid radial dimensions");this.radius=radius;this.buttonSize=buttonSize;invalidateLayout();return this; }
    public Radial item(UIComponent item) { items.add(item); addChild(item); return this; }
    public Radial center(UIComponent component,int size) { if(size<=0) throw new IllegalArgumentException("Invalid center size");if(center!=null)removeChild(center);center=component;centerSize=size;addChild(component);return this; }
    @Override public int preferredWidth(Font font) { return radius*2+buttonSize; }
    @Override public int preferredHeight(Font font) { return radius*2+buttonSize; }
    @Override public void layout(int x,int y,int width,int height) {
        setBounds(x,y,width,height);int cx=x+width/2,cy=y+height/2;
        for(int i=0;i<items.size();i++) items.get(i).layout(cx+RadialLayout.x(i,items.size(),radius)-buttonSize/2,cy+RadialLayout.y(i,items.size(),radius)-buttonSize/2,buttonSize,buttonSize);
        if(center!=null) center.layout(cx-centerSize/2,cy-centerSize/2,centerSize,centerSize);
    }
    @Override public void render(GuiGraphics g,Font font,int mx,int my,float dt) { renderChildren(g,font,mx,my,dt); }
}
