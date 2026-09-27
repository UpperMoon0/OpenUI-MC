package com.nstut.openui.api;

import com.nstut.openui.debug.LayoutDiagnostics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class Panel extends UIComponent {
    private Integer backgroundOverride;
    private Integer borderOverride;
    private int customRadius = -1;
    private int customPadding = -1;
    private boolean elevated;

    public Panel(int bgColor, int borderColor) { this.backgroundOverride = bgColor; this.borderOverride = borderColor; }
    public Panel(int bgColor) { this.backgroundOverride = bgColor; }
    public Panel() { }
    public Panel radius(int radius) { int next = Math.max(0, radius); if (customRadius == next) return this; customRadius = next; invalidateLayout(); return this; }
    public Panel themeRadius() { if (customRadius < 0) return this; customRadius = -1; invalidateLayout(); return this; }
    public Panel padding(int padding) { int next = Math.max(0, padding); if (customPadding == next) return this; customPadding = next; invalidateLayout(); return this; }
    public Panel themePadding() { if (customPadding < 0) return this; customPadding = -1; invalidateLayout(); return this; }
    public Panel elevated() { if (this.elevated) return this; this.elevated = true; invalidateLayout(); return this; }
    public Panel child(UIComponent child) { addChild(child); return this; }
    public Panel colors(int background, int border) { this.backgroundOverride = background; this.borderOverride = border; invalidateLayout(); return this; }
    public Panel themeColors() { backgroundOverride = null; borderOverride = null; invalidateLayout(); return this; }
    private int effectiveRadius() { return customRadius >= 0 ? customRadius : theme().radii().medium(); }
    private int effectivePadding() { return customPadding >= 0 ? customPadding : theme().spacing().sm(); }
    int effectiveBackground() { return backgroundOverride != null ? backgroundOverride : theme().colors().surfaceRaised(); }
    int effectiveBorder() { return borderOverride != null ? borderOverride : (elevated ? theme().colors().border() : 0); }

    @Override public int preferredWidth(Font font) { int inset=contentInset(), max=0; for (UIComponent c:children) max=Math.max(max,c.preferredWidth(font)); return max+inset*2; }
    @Override public int preferredHeight(Font font) { int inset=contentInset(), max=0; for (UIComponent c:children) max=Math.max(max,c.preferredHeight(font)); return max+inset*2; }

    @Override public void layout(int x,int y,int availableWidth,int availableHeight) {
        setBounds(x,y,availableWidth,availableHeight); int inset=contentInset();
        int innerW=Math.max(0,availableWidth-inset*2), innerH=Math.max(0,availableHeight-inset*2);
        for (UIComponent c:children) {
            LayoutDiagnostics.checkBoundedOverflow(this,c,innerW,innerH,measureFont());
            c.layout(x+inset,y+inset,innerW,innerH);
        }
    }

    private int contentInset() { return Math.max(effectivePadding(), roundedContentInset(effectiveRadius(),effectiveBorder()!=0?1:0)); }
    private static int roundedContentInset(int radius,int borderWidth) {
        int border=Math.max(0,borderWidth), innerRadius=Math.max(0,radius-border);
        return border+cornerSafeInset(innerRadius);
    }
    private static int cornerSafeInset(int radius) {
        int r=Math.max(0,radius);
        if(r<=1) return 0;
        double rr=(double)r*r;
        return Math.max(0,(int)Math.ceil(r-(1.0D+Math.sqrt(8.0D*rr-1.0D))/4.0D));
    }
    private int childClipInset() { return contentInset(); }
    @Override protected boolean clipsChildrenToBounds() { return true; }
    @Override protected int childClipX() { return x + childClipInset(); }
    @Override protected int childClipY() { return y + childClipInset(); }
    @Override protected int childClipWidth() { int inset=childClipInset(); return Math.max(0,width-inset*2); }
    @Override protected int childClipHeight() { int inset=childClipInset(); return Math.max(0,height-inset*2); }

    @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        var colors=theme().colors();
        int bg=effectiveBackground();
        int border=effectiveBorder();
        UiRender.surface(g,x,y,width,height,effectiveRadius(),bg,border,elevated,colors);
        renderChildren(g,font,mx,my,pt);
    }

    @Override public boolean mouseClicked(double mx,double my,int button) { return childrenMouseClicked(mx,my,button); }
    @Override public boolean mouseScrolled(double mx,double my,double delta) { return childrenMouseScrolled(mx,my,delta); }
    @Override public boolean mouseDragged(double mx,double my,int button,double dragX,double dragY) { return childrenMouseDragged(mx,my,button,dragX,dragY); }
    @Override public boolean mouseReleased(double mx,double my,int button) { return childrenMouseReleased(mx,my,button); }
}
