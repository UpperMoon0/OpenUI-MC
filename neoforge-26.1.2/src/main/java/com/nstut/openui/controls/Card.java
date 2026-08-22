package com.nstut.openui.controls;

import com.nstut.openui.animation.Easing;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Card extends UIComponent {
    private boolean hoverable = true;
    private boolean clickable = false;
    private boolean selected = false;
    private Boolean elevatedOverride;
    private boolean outlined = true;
    private int customPadding = -1;
    private int customRadius = -1;
    private Runnable onClick;
    private float hoverProgress;
    private long lastAnimTime = -1;

    public Card() { focusable(false); }
    public Card(UIComponent child) { this(); if (child != null) addChild(child); }
    public Card hoverable(boolean hoverable) { this.hoverable = hoverable; return this; }
    public Card clickable(boolean clickable) { this.clickable = clickable; focusable(clickable); return this; }
    public Card onClick(Runnable onClick) { this.onClick = onClick; return clickable(true); }
    public Card selected(boolean selected) { this.selected = selected; invalidatePaint(); return this; }
    public Card elevated(boolean elevated) { if (java.util.Objects.equals(elevatedOverride, elevated)) return this; elevatedOverride = elevated; invalidatePaint(); return this; }
    public Card themeElevation() { if (elevatedOverride == null) return this; elevatedOverride = null; invalidatePaint(); return this; }
    public Card outlined(boolean outlined) { this.outlined = outlined; invalidatePaint(); return this; }
    public Card padding(int padding) { this.customPadding = Math.max(0,padding); invalidateLayout(); return this; }
    public Card radius(int radius) { this.customRadius = Math.max(0,radius); invalidatePaint(); return this; }

    @Override public int preferredWidth(Font font) {
        int pad=customPadding>=0?customPadding:theme().cardTheme().padding();
        int max=0; for(UIComponent child:children) max=Math.max(max,child.preferredWidth(font));
        return max+pad*2;
    }
    @Override public int preferredHeight(Font font) {
        int pad=customPadding>=0?customPadding:theme().cardTheme().padding();
        int total=0; for(UIComponent child:children) total+=child.preferredHeight(font);
        return total+pad*2;
    }
    @Override public void layout(int x,int y,int availableWidth,int availableHeight) {
        setBounds(x,y,availableWidth,availableHeight);
        int pad=customPadding>=0?customPadding:theme().cardTheme().padding();
        int innerX=x+pad, innerY=y+pad;
        int innerW=Math.max(0,availableWidth-pad*2), innerH=Math.max(0,availableHeight-pad*2);
        for(UIComponent child:children) child.layoutTree(measureFont(),innerX,innerY,innerW,innerH);
    }
    @Override public void render(GuiGraphicsExtractor g,Font font,int mx,int my,float pt) {
        if(!visible) return;
        Theme t=theme(); ColorScheme colors=t.colors();
        long now=System.nanoTime(); if(lastAnimTime<0) lastAnimTime=now;
        float dt=(now-lastAnimTime)/1_000_000_000.0F; lastAnimTime=now;
        float target=(hoverable&&isHovered())?1.0F:0.0F;
        if(t.reducedMotion()||t.durations().hoverMs()<=0) hoverProgress=target;
        else { float duration=t.durations().hoverMs()/1000.0F; float step=dt/duration;
            if(hoverProgress<target) hoverProgress=Math.min(target,hoverProgress+step);
            else if(hoverProgress>target) hoverProgress=Math.max(target,hoverProgress-step); }
        float eased=Easing.EASE_OUT.apply(hoverProgress);
        int radius=customRadius>=0?customRadius:t.cardTheme().radius();
        int baseBg=selected?colors.surfaceVariant():colors.surfaceRaised();
        int bg=UiRender.mix(baseBg,colors.surfaceVariant(),eased);
        int baseBorder=selected?colors.primary():(outlined?colors.border():0);
        int hoverBorder=selected?colors.primaryHover():(outlined?colors.borderStrong():0);
        int border=UiRender.mix(baseBorder,hoverBorder,eased);
        if(isFocused()) border=colors.primary();
        boolean elevated = elevatedOverride != null ? elevatedOverride : t.cardTheme().elevated();
        if(elevated) UiRender.shadow(g,x,y,width,height,radius,colors);
        UiRender.roundedOutline(g,x,y,width,height,radius,bg,border);
        for(UIComponent child:children) child.render(g,font,mx,my,pt);
    }
    @Override public boolean mouseClicked(double mx,double my,int btn) {
        if(clickable&&btn==0&&isHovered()&&onClick!=null) { onClick.run(); return true; }
        return super.mouseClicked(mx,my,btn);
    }
    @Override public boolean keyPressed(int key,int scanCode,int modifiers) {
        if(clickable&&isFocused()&&(key==257||key==32)&&onClick!=null) { onClick.run(); return true; }
        return super.keyPressed(key,scanCode,modifiers);
    }
}
