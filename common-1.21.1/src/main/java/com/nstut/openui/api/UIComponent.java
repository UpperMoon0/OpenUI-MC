package com.nstut.openui.api;

import com.nstut.openui.component.DirtyFlag;
import com.nstut.openui.component.LifecycleState;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Size;
import com.nstut.openui.input.EventPhase;
import com.nstut.openui.input.EventType;
import com.nstut.openui.input.UiEvent;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.List;

public abstract class UIComponent {

    protected int x, y, width, height;
    protected UIComponent parent;
    protected final List<UIComponent> children = new ArrayList<>();
    protected boolean hovered;
    protected boolean visible = true;
    protected boolean flex;
    protected float flexGrow;
    private Font measureFont;
    private UiRuntime runtime;
    private LifecycleState lifecycleState = LifecycleState.CREATED;
    private final EnumSet<DirtyFlag> dirtyFlags = EnumSet.allOf(DirtyFlag.class);
    private int minWidth;
    private int maxWidth = Constraints.INFINITY;
    private int minHeight;
    private int maxHeight = Constraints.INFINITY;
    private int requestedWidth = -1;
    private int requestedHeight = -1;
    private boolean fillWidth;
    private boolean fillHeight;
    private boolean focusable;
    private String key;
    private Theme localTheme;
    private final EnumMap<EventType, List<Consumer<UiEvent>>> captureListeners = new EnumMap<>(EventType.class);
    private final EnumMap<EventType, List<Consumer<UiEvent>>> bubbleListeners = new EnumMap<>(EventType.class);

    public UIComponent flex() { return flex(1.0F); }
    public UIComponent flex(float grow) {
        this.flex = grow > 0.0F;
        this.flexGrow = Math.max(0.0F, grow);
        return this;
    }
    public boolean isFlex() { return flexGrow > 0.0F || flex; }
    public float getFlexGrow() { return flexGrow > 0.0F ? flexGrow : (flex ? 1.0F : 0.0F); }

    public void setVisible(boolean v) {
        if (visible == v) return;
        this.visible = v;
        invalidateLayout();
    }
    public boolean isVisible() { return visible; }

    public void addChild(UIComponent child) {
        if (child == null) return;
        if (child.parent != null) child.parent.removeChild(child);
        children.add(child);
        child.parent = this;
        if (runtime != null) child.mount(runtime);
        invalidateBuild();
    }

    public boolean removeChild(UIComponent child) {
        if (!children.remove(child)) return false;
        child.unmount();
        child.parent = null;
        invalidateBuild();
        return true;
    }

    public void clearChildren() {
        for (UIComponent child : List.copyOf(children)) removeChild(child);
    }

    public int childCount() { return children.size(); }
    public UIComponent child(int i) { return children.get(i); }
    public void replaceChild(int i, UIComponent child) {
        UIComponent old = children.set(i, child);
        old.unmount();
        old.parent = null;
        child.parent = this;
        if (runtime != null) child.mount(runtime);
        invalidateBuild();
    }
    public List<UIComponent> children() { return Collections.unmodifiableList(children); }
    public UIComponent parent() { return parent; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String key() { return key; }
    public UIComponent key(String key) { this.key = key; return this; }

    public UIComponent width(int value) { requestedWidth = Math.max(0, value); invalidateLayout(); return this; }
    public UIComponent height(int value) { requestedHeight = Math.max(0, value); invalidateLayout(); return this; }
    public UIComponent minWidth(int value) { minWidth = Math.max(0, value); invalidateLayout(); return this; }
    public UIComponent maxWidth(int value) { maxWidth = Math.max(minWidth, value); invalidateLayout(); return this; }
    public UIComponent minHeight(int value) { minHeight = Math.max(0, value); invalidateLayout(); return this; }
    public UIComponent maxHeight(int value) { maxHeight = Math.max(minHeight, value); invalidateLayout(); return this; }
    public UIComponent fillWidth() { fillWidth = true; invalidateLayout(); return this; }
    public UIComponent fillHeight() { fillHeight = true; invalidateLayout(); return this; }

    public Size measure(Constraints constraints, Font font) {
        int desiredWidth = requestedWidth >= 0 ? requestedWidth : preferredWidth(font);
        int desiredHeight = requestedHeight >= 0 ? requestedHeight : preferredHeight(font);
        if (fillWidth) desiredWidth = constraints.maxWidth();
        if (fillHeight) desiredHeight = constraints.maxHeight();
        desiredWidth = Math.max(minWidth, Math.min(maxWidth, desiredWidth));
        desiredHeight = Math.max(minHeight, Math.min(maxHeight, desiredHeight));
        return constraints.constrain(new Size(desiredWidth, desiredHeight));
    }

    public abstract int preferredWidth(Font font);
    public abstract int preferredHeight(Font font);

    /**
     * Measures and lays out an entire component tree with the real screen font.
     * Existing layout implementations stay source-compatible while no longer
     * having to guess text sizes with a null font.
     */
    public final void layoutTree(Font font, int x, int y, int availableWidth, int availableHeight) {
        setMeasureFont(font);
        layout(x, y, availableWidth, availableHeight);
        dirtyFlags.remove(DirtyFlag.LAYOUT);
    }

    private void setMeasureFont(Font font) {
        this.measureFont = font;
        for (UIComponent child : children) child.setMeasureFont(font);
    }

    protected final Font measureFont() { return measureFont; }

    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
    }

    protected void setBounds(int x, int y, int w, int h) {
        this.x = x; this.y = y; this.width = w; this.height = h;
    }

    public final void mount(UiRuntime runtime) {
        if (lifecycleState == LifecycleState.MOUNTED && this.runtime == runtime) return;
        if (lifecycleState == LifecycleState.MOUNTED) unmount();
        this.runtime = runtime;
        lifecycleState = LifecycleState.MOUNTED;
        onMount();
        for (UIComponent child : children) child.mount(runtime);
    }

    public final void unmount() {
        if (lifecycleState != LifecycleState.MOUNTED) return;
        for (UIComponent child : children) child.unmount();
        onUnmount();
        runtime = null;
        lifecycleState = LifecycleState.UNMOUNTED;
    }

    protected void onMount() { }
    protected void onUnmount() { }
    protected final UiRuntime runtime() { return runtime; }
    public LifecycleState lifecycleState() { return lifecycleState; }

    public final void invalidateBuild() {
        dirtyFlags.add(DirtyFlag.BUILD);
        invalidateLayout();
    }

    public final void invalidateLayout() {
        dirtyFlags.add(DirtyFlag.LAYOUT);
        dirtyFlags.add(DirtyFlag.PAINT);
        if (parent != null) parent.invalidateLayout();
        else if (runtime != null) runtime.requestLayout();
    }

    public final void invalidatePaint() {
        dirtyFlags.add(DirtyFlag.PAINT);
        if (runtime != null) runtime.requestPaint();
    }

    public boolean isDirty(DirtyFlag flag) { return dirtyFlags.contains(flag); }
    public void markPainted() { dirtyFlags.remove(DirtyFlag.PAINT); }
    public void markBuilt() { dirtyFlags.remove(DirtyFlag.BUILD); }

    public UIComponent focusable(boolean focusable) { this.focusable = focusable; return this; }
    public boolean isFocusable() { return focusable && visible; }
    public boolean isFocused() { return runtime != null && runtime.focus().focused() == this; }
    public void requestFocus() { if (runtime != null) runtime.focus().requestFocus(this); }
    public void clearFocus() { if (runtime != null && isFocused()) runtime.focus().clearFocus(); }
    public UIComponent theme(Theme theme) { this.localTheme = theme; invalidatePaint(); return this; }
    public Theme theme() {
        if (localTheme != null) return localTheme;
        if (parent != null) return parent.theme();
        return runtime != null ? runtime.theme() : Theme.dark();
    }

    public UIComponent on(EventType type, Consumer<UiEvent> listener) {
        bubbleListeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
        return this;
    }

    public UIComponent onCapture(EventType type, Consumer<UiEvent> listener) {
        captureListeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
        return this;
    }

    public void dispatchEvent(UiEvent event, EventPhase phase) {
        List<Consumer<UiEvent>> listeners = phase == EventPhase.CAPTURE
                ? captureListeners.get(event.type()) : bubbleListeners.get(event.type());
        if (listeners == null) return;
        for (Consumer<UiEvent> listener : List.copyOf(listeners)) {
            listener.accept(event);
            if (event.isPropagationStopped()) return;
        }
    }

    public abstract void render(GuiGraphics g, Font font, int mx, int my, float pt);

    public boolean mouseClicked(double mx, double my, int button) { return false; }
    public boolean mouseScrolled(double mx, double my, double delta) { return false; }
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) { return false; }
    public boolean mouseReleased(double mx, double my, int button) { return false; }
    public boolean keyPressed(int key, int scanCode, int modifiers) { return false; }
    public boolean keyReleased(int key, int scanCode, int modifiers) { return false; }
    public boolean charTyped(char character, int modifiers) { return false; }

    public UIComponent hitTest(int mx, int my) {
        if (!visible) return null;
        if (mx >= x && mx < x + width && my >= y && my < y + height) {
            for (int i = children.size() - 1; i >= 0; i--) {
                UIComponent hit = children.get(i).hitTest(mx, my);
                if (hit != null) return hit;
            }
            return this;
        }
        return null;
    }

    public void preRender(int mx, int my) {
        if (!visible) return;
        hovered = mx >= x && mx < x + width && my >= y && my < y + height;
        for (UIComponent c : children) c.preRender(mx, my);
    }

    public boolean isHovered() { return hovered; }

    protected final void renderChildren(GuiGraphics g, Font font, int mx, int my, float pt) {
        for (UIComponent child : children) {
            if (child.isVisible()) child.render(g, font, mx, my, pt);
        }
    }

    protected final boolean childrenMouseClicked(double mx, double my, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child.isVisible() && child.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    protected final boolean childrenMouseScrolled(double mx, double my, double delta) {
        for (UIComponent child : children) {
            if (child.isVisible() && child.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    protected final boolean childrenMouseDragged(double mx, double my, int button, double dragX, double dragY) {
        for (UIComponent child : children) {
            if (child.isVisible() && child.mouseDragged(mx, my, button, dragX, dragY)) return true;
        }
        return false;
    }

    protected final boolean childrenMouseReleased(double mx, double my, int button) {
        for (UIComponent child : children) {
            if (child.isVisible() && child.mouseReleased(mx, my, button)) return true;
        }
        return false;
    }

    public void dispose() {
        unmount();
        for (UIComponent c : children) c.dispose();
        children.clear();
    }
}
