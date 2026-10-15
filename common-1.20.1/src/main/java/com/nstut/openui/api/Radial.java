package com.nstut.openui.api;
import com.nstut.openui.layout.RadialLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
/** Radial arrangement of ordinary OpenUI controls, retaining their normal focus and input semantics. */
public final class Radial extends UIComponent {
    private int radius,buttonSize,centerSize;
    private UIComponent center;
    public Radial(int radius,int buttonSize) { dimensions(radius,buttonSize); }
    public Radial dimensions(int radius,int buttonSize) { if(radius<0 || buttonSize<=0) throw new IllegalArgumentException("Invalid radial dimensions");this.radius=radius;this.buttonSize=buttonSize;invalidateLayout();return this; }
    public Radial item(UIComponent item) { java.util.Objects.requireNonNull(item); addChild(item); return this; }
    public Radial center(UIComponent component,int size) { if(size<=0) throw new IllegalArgumentException("Invalid center size");java.util.Objects.requireNonNull(component);if(center!=null)removeChild(center);addChild(component);center=component;centerSize=size;return this; }
    @Override public boolean removeChild(UIComponent child) {
        if (!children().contains(child)) return false;
        if (center == child) center = null;
        return super.removeChild(child);
    }
    @Override public void replaceChild(int index, UIComponent replacement) {
        java.util.Objects.requireNonNull(replacement);
        if (child(index) == center) center = replacement;
        super.replaceChild(index, replacement);
    }
    @Override public int preferredWidth(Font font) { return radius*2+buttonSize; }
    @Override public int preferredHeight(Font font) { return radius*2+buttonSize; }
    @Override public void layout(int x,int y,int width,int height) {
        setBounds(x,y,width,height);int cx=x+width/2,cy=y+height/2;
        int count=childCount()-(center!=null?1:0), index=0;
        for(UIComponent item:children()) {
            if(item==center) continue;
            item.layout(cx+RadialLayout.x(index,count,radius)-buttonSize/2,cy+RadialLayout.y(index,count,radius)-buttonSize/2,buttonSize,buttonSize);
            index++;
        }
        if(center!=null) center.layout(cx-centerSize/2,cy-centerSize/2,centerSize,centerSize);
    }
    @Override public void render(GuiGraphics g,Font font,int mx,int my,float dt) { renderChildren(g,font,mx,my,dt); }
}
