package com.nstut.openui.controls;

import com.nstut.openui.animation.Easing;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.debug.LayoutDiagnostics;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

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
    public Card selected(boolean selected) { if (this.selected == selected) return this; this.selected = selected; invalidatePaint(); return this; }
    public Card elevated(boolean elevated) { if (java.util.Objects.equals(elevatedOverride, elevated)) return this; elevatedOverride = elevated; invalidatePaint(); return this; }
    public Card themeElevation() { if (elevatedOverride == null) return this; elevatedOverride = null; invalidatePaint(); return this; }
    public Card outlined(boolean outlined) { if (this.outlined == outlined) return this; this.outlined = outlined; invalidatePaint(); return this; }
    public Card padding(int padding) { this.customPadding = Math.max(0,padding); invalidateLayout(); return this; }
    public Card radius(int radius) { int next=Math.max(0,radius); if(this.customRadius==next) return this; this.customRadius=next; invalidateLayout(); return this; }

    @Override public int preferredWidth(Font font) { return intrinsicSurfaceSize(font).width(); }
    @Override public int preferredHeight(Font font) {
        if(width<=0) return intrinsicSurfaceSize(font).height();
        int candidateHeight=Math.max(0,intrinsicContentHeight(font))+effectivePadding()*2;
        for(int i=0;i<16;i++) {
            int inset=contentInsetForBounds(width,candidateHeight);
            int innerWidth=Math.max(0,width-inset*2);
            int contentHeight=0;
            for(UIComponent child:children) {
                contentHeight+=child.measure(Constraints.loose(innerWidth,Constraints.INFINITY),font).height();
            }
            int next=Math.max(0,contentHeight+inset*2);
            if(next==candidateHeight) break;
            candidateHeight=next;
        }
        return candidateHeight;
    }
    @Override public Size measure(Constraints constraints, Font font) {
        Size initial=super.measure(constraints,font);
        int previousWidth=width;
        width=initial.width();
        try {
            return super.measure(constraints,font);
        } finally {
            width=previousWidth;
        }
    }
    @Override public void layout(int x,int y,int availableWidth,int availableHeight) {
        setBounds(x,y,availableWidth,availableHeight);
        int inset=contentInsetForBounds(availableWidth,availableHeight);
        int innerX=x+inset, innerY=y+inset;
        int innerW=Math.max(0,availableWidth-inset*2), innerH=Math.max(0,availableHeight-inset*2);
        for(UIComponent child:children) {
            LayoutDiagnostics.checkBoundedOverflow(this,child,innerW,innerH,measureFont());
            child.layoutTree(measureFont(),innerX,innerY,innerW,innerH);
        }
    }
    private static final int STATE_BORDER_WIDTH = 1;
    private int effectivePadding() { return customPadding>=0?customPadding:theme().cardTheme().padding(); }
    private int effectiveRadius() { return customRadius>=0?customRadius:theme().cardTheme().radius(); }
    private Size intrinsicSurfaceSize(Font font) {
        int contentWidth=0;
        for(UIComponent child:children) contentWidth=Math.max(contentWidth,child.preferredWidth(font));
        int contentHeight=intrinsicContentHeight(font);
        int candidateWidth=Math.max(0,contentWidth+effectivePadding()*2);
        int candidateHeight=Math.max(0,contentHeight+effectivePadding()*2);
        for(int i=0;i<16;i++) {
            int inset=contentInsetForBounds(candidateWidth,candidateHeight);
            int nextWidth=Math.max(0,contentWidth+inset*2);
            int nextHeight=Math.max(0,contentHeight+inset*2);
            if(nextWidth==candidateWidth&&nextHeight==candidateHeight) break;
            candidateWidth=nextWidth;
            candidateHeight=nextHeight;
        }
        return new Size(candidateWidth,candidateHeight);
    }
    private int intrinsicContentHeight(Font font) {
        int total=0;
        for(UIComponent child:children) total+=child.preferredHeight(font);
        return total;
    }
    private int contentInsetForBounds(int surfaceWidth,int surfaceHeight) { return Math.max(effectivePadding(),roundedContentInset(effectiveRadius(),STATE_BORDER_WIDTH,surfaceWidth,surfaceHeight)); }
    private static int roundedContentInset(int radius,int borderWidth,int surfaceWidth,int surfaceHeight) {
        int border=Math.max(0,borderWidth);
        int clampedRadius=clampRadius(radius,surfaceWidth,surfaceHeight);
        int innerRadius=Math.max(0,clampedRadius-border);
        return border+cornerSafeInset(innerRadius);
    }
    private static int clampRadius(int radius,int width,int height) {
        int maxRadius=Math.max(0,Math.min(width,height)/2);
        return Math.min(Math.max(0,radius),maxRadius);
    }
    private static int cornerSafeInset(int radius) {
        int r=Math.max(0,radius);
        if(r<=1) return 0;
        double rr=(double)r*r;
        return Math.max(0,(int)Math.ceil(r-(1.0D+Math.sqrt(8.0D*rr-1.0D))/4.0D));
    }
    private int childClipInset() { return contentInsetForBounds(width,height); }
    @Override protected boolean clipsChildrenToBounds() { return true; }
    @Override protected int childClipX() { return x + childClipInset(); }
    @Override protected int childClipY() { return y + childClipInset(); }
    @Override protected int childClipWidth() { int inset=childClipInset(); return Math.max(0,width-inset*2); }
    @Override protected int childClipHeight() { int inset=childClipInset(); return Math.max(0,height-inset*2); }
    @Override public void render(GuiGraphics g,Font font,int mx,int my,float pt) {
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
        int radius=effectiveRadius();
        int baseBg=selected?colors.surfaceVariant():colors.surfaceRaised();
        int bg=UiRender.mix(baseBg,colors.surfaceVariant(),eased);
        int baseBorder=selected?colors.primary():(outlined?colors.border():0);
        int hoverBorder=selected?colors.primaryHover():(outlined?colors.borderStrong():0);
        int border=UiRender.mix(baseBorder,hoverBorder,eased);
        if(isFocused()) border=colors.primary();
        boolean elevated = elevatedOverride != null ? elevatedOverride : t.cardTheme().elevated();
        if(elevated) UiRender.shadow(g,x,y,width,height,radius,colors);
        UiRender.roundedOutline(g,x,y,width,height,radius,bg,border);
        renderChildren(g,font,mx,my,pt);
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
