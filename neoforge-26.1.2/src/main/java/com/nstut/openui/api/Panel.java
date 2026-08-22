package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Panel extends UIComponent {
    private Integer backgroundOverride;
    private Integer borderOverride;
    private int customRadius = -1;
    private int customPadding = -1;
    private boolean elevated;

    public Panel(int bgColor, int borderColor) { this.backgroundOverride = bgColor; this.borderOverride = borderColor; }
    public Panel(int bgColor) { this(bgColor, 0); }
    public Panel() { this(0, 0); }
    public Panel radius(int radius) { int next = Math.max(0, radius); if (customRadius == next) return this; customRadius = next; invalidatePaint(); return this; }
    public Panel themeRadius() { if (customRadius < 0) return this; customRadius = -1; invalidatePaint(); return this; }
    public Panel padding(int padding) { int next = Math.max(0, padding); if (customPadding == next) return this; customPadding = next; invalidateLayout(); return this; }
    public Panel themePadding() { if (customPadding < 0) return this; customPadding = -1; invalidateLayout(); return this; }
    public Panel elevated() { this.elevated = true; invalidatePaint(); return this; }
    public Panel child(UIComponent child) { addChild(child); return this; }
    public Panel colors(int background, int border) { this.backgroundOverride = background; this.borderOverride = border; invalidatePaint(); return this; }
    public Panel themeColors() { backgroundOverride = null; borderOverride = null; invalidatePaint(); return this; }
    private int effectiveRadius() { return customRadius >= 0 ? customRadius : theme().radii().medium(); }
    private int effectivePadding() { return customPadding >= 0 ? customPadding : theme().spacing().sm(); }

    @Override public int preferredWidth(Font font) { int padding=effectivePadding(), max=0; for (UIComponent c:children) max=Math.max(max,c.preferredWidth(font)); return max+padding*2; }
    @Override public int preferredHeight(Font font) { int padding=effectivePadding(), max=0; for (UIComponent c:children) max=Math.max(max,c.preferredHeight(font)); return max+padding*2; }

    @Override public void layout(int x,int y,int availableWidth,int availableHeight) {
        setBounds(x,y,availableWidth,availableHeight); int padding=effectivePadding();
        for (UIComponent c:children) c.layout(x+padding,y+padding,Math.max(0,availableWidth-padding*2),Math.max(0,availableHeight-padding*2));
    }

    @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        var colors=theme().colors();
        int bg=backgroundOverride != null ? backgroundOverride : colors.surfaceRaised();
        int border=borderOverride != null ? borderOverride : (elevated?colors.border():0);
        UiRender.surface(g,x,y,width,height,effectiveRadius(),bg,border,elevated,colors);
        renderChildren(g,font,mx,my,pt);
    }

    @Override public boolean mouseClicked(double mx,double my,int button) { return childrenMouseClicked(mx,my,button); }
    @Override public boolean mouseScrolled(double mx,double my,double delta) { return childrenMouseScrolled(mx,my,delta); }
    @Override public boolean mouseDragged(double mx,double my,int button,double dragX,double dragY) { return childrenMouseDragged(mx,my,button,dragX,dragY); }
    @Override public boolean mouseReleased(double mx,double my,int button) { return childrenMouseReleased(mx,my,button); }
}
