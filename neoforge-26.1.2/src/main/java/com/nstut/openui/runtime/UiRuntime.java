package com.nstut.openui.runtime;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.input.EventPhase;
import com.nstut.openui.input.EventType;
import com.nstut.openui.input.KeyboardEvent;
import com.nstut.openui.input.PointerEvent;
import com.nstut.openui.input.UiEvent;
import com.nstut.openui.animation.AnimationManager;
import com.nstut.openui.theme.Theme;
import com.nstut.openui.overlay.OverlayManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

public final class UiRuntime implements AutoCloseable {
    private final Font font;
    private final FocusManager focus = new FocusManager();
    private final NativeWidgetManager nativeWidgets;
    private final AnimationManager animations = new AnimationManager();
    private final OverlayManager overlays = new OverlayManager(this);
    private Theme theme;
    private UIComponent root;
    private boolean layoutDirty = true;
    private boolean overlayLayoutDirty = true;
    private boolean paintDirty = true;
    private int x;
    private int y;
    private int width;
    private int height;
    private UIComponent pointerCapture;
    private UIComponent pressedTarget;

    public UiRuntime(Font font, NativeWidgetHost widgetHost) {
        this(font, widgetHost, Theme.dark());
    }

    public UiRuntime(Font font, NativeWidgetHost widgetHost, Theme theme) {
        this.font = Objects.requireNonNull(font);
        this.nativeWidgets = new NativeWidgetManager(Objects.requireNonNull(widgetHost));
        this.theme = Objects.requireNonNull(theme);
        this.focus.setOverlayRoots(this.overlays::components);
    }

    public UIComponent root() { return root; }
    public FocusManager focus() { return focus; }
    public NativeWidgetManager nativeWidgets() { return nativeWidgets; }
    public AnimationManager animations() { return animations; }
    public OverlayManager overlays() { return overlays; }
    public Theme theme() { return theme; }
    public void theme(Theme theme) {
        Theme next = Objects.requireNonNull(theme);
        if (Objects.equals(this.theme, next)) return;
        this.theme = next;
        requestLayout();
    }

    public void setRoot(UIComponent root) {
        if (this.root == root) return;
        if (this.root != null) this.root.unmount();
        this.root = Objects.requireNonNull(root);
        this.root.mount(this);
        focus.setRoot(root);
        requestLayout();
        nativeWidgets.synchronize(root, overlays.components());
    }

    public void setViewport(int x, int y, int width, int height) {
        if (this.x == x && this.y == y && this.width == width && this.height == height) return;
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        requestLayout();
    }

    public void requestLayout() { layoutDirty = true; overlayLayoutDirty = true; paintDirty = true; }
    /** Invalidates overlay geometry without re-laying out the application root. */
    public void requestOverlayLayout() { overlayLayoutDirty = true; paintDirty = true; }
    public void requestPaint() { paintDirty = true; }
    public boolean isLayoutDirty() { return layoutDirty; }
    public boolean isPaintDirty() { return paintDirty; }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (root == null) return;
        animations.tick(System.nanoTime());
        nativeWidgets.synchronize(root, overlays.components());
        if (layoutDirty) {
            root.layoutTree(font, x, y, width, height);
            layoutDirty = false;
        }
        if (overlayLayoutDirty) {
            overlays.layout(font, x, y, width, height);
            overlayLayoutDirty = false;
        }
        root.preRender(mouseX, mouseY);
        root.render(graphics, font, mouseX, mouseY, partialTick);
        overlays.render(graphics, font, mouseX, mouseY, partialTick);
        root.markPainted();
        paintDirty = false;
    }

    public void preRender(int mouseX, int mouseY) {
        if (root != null) root.preRender(mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (root == null) return false;
        boolean hasBlocking = overlays.hasBlockingOverlay();
        UIComponent target = inputTarget(mouseX, mouseY);
        UIComponent focusTarget = target;
        while (focusTarget != null && !focusTarget.isFocusable()) {
            focusTarget = focusTarget.parent();
        }
        if (focusTarget != null && focusTarget.isFocusable()) focus.requestFocus(focusTarget);
        else if (button == 0) focus.clearFocus();
        if (target == null) return hasBlocking;
        pressedTarget = target;
        PointerEvent event = new PointerEvent(EventType.MOUSE_DOWN, target, mouseX, mouseY, button, 0, 0);
        dispatch(event);
        if (event.isPointerCaptureRequested()) pointerCapture = event.pointerCaptureTarget();
        boolean handled = false;
        if (!event.isDefaultPrevented() && !event.isPropagationStopped()) {
            handled = bubbleMouseClicked(target, mouseX, mouseY, button);
        }
        if (hasBlocking) handled = true;
        return handled || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        UIComponent target = inputTarget(mouseX, mouseY);
        if (target == null) return false;
        PointerEvent event = new PointerEvent(EventType.SCROLL, target, mouseX, mouseY, -1, 0, delta);
        dispatch(event);
        boolean handled = !event.isDefaultPrevented() && !event.isPropagationStopped() && bubbleMouseScrolled(target, mouseX, mouseY, delta);
        return handled || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        UIComponent target = pointerCapture != null ? pointerCapture : inputTarget(mouseX, mouseY);
        if (target == null) return false;
        PointerEvent event = new PointerEvent(EventType.MOUSE_DRAG, target, mouseX, mouseY, button, dragX, dragY);
        dispatch(event);
        if (event.isPointerCaptureRequested()) pointerCapture = event.pointerCaptureTarget();
        boolean handled = !event.isDefaultPrevented() && !event.isPropagationStopped() && target.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return handled || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        UIComponent target = pointerCapture != null ? pointerCapture : inputTarget(mouseX, mouseY);
        if (target == null) return false;
        PointerEvent event = new PointerEvent(EventType.MOUSE_UP, target, mouseX, mouseY, button, 0, 0);
        dispatch(event);
        boolean handled = !event.isDefaultPrevented() && !event.isPropagationStopped() && target.mouseReleased(mouseX, mouseY, button);
        UIComponent hit = inputTarget(mouseX, mouseY);
        if (pressedTarget != null && pressedTarget == hit) {
            PointerEvent click = new PointerEvent(EventType.CLICK, hit, mouseX, mouseY, button, 0, 0);
            dispatch(click);
            handled |= click.isDefaultPrevented() || click.isPropagationStopped();
        }
        pointerCapture = null;
        pressedTarget = null;
        return handled || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256 && overlays.closeTopDismissable()) return true;
        if (key == 258) return (modifiers & 1) != 0 ? focus.focusPrevious() : focus.focusNext();
        UIComponent target = focus.focused();
        if (target == null) return false;
        KeyboardEvent event = new KeyboardEvent(EventType.KEY_DOWN, target, key, scanCode, modifiers, '\0');
        dispatch(event);
        return !event.isDefaultPrevented() && target.keyPressed(key, scanCode, modifiers)
                || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public boolean keyReleased(int key, int scanCode, int modifiers) {
        UIComponent target = focus.focused();
        if (target == null) return false;
        KeyboardEvent event = new KeyboardEvent(EventType.KEY_UP, target, key, scanCode, modifiers, '\0');
        dispatch(event);
        return !event.isDefaultPrevented() && target.keyReleased(key, scanCode, modifiers)
                || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public boolean charTyped(char character, int modifiers) {
        UIComponent target = focus.focused();
        if (target == null) return false;
        KeyboardEvent event = new KeyboardEvent(EventType.CHAR_TYPED, target, 0, 0, modifiers, character);
        dispatch(event);
        return !event.isDefaultPrevented() && target.charTyped(character, modifiers)
                || event.isDefaultPrevented() || event.isPropagationStopped();
    }

    public void dispatch(UiEvent event) {
        List<UIComponent> path = new ArrayList<>();
        UIComponent cursor = event.target();
        while (cursor != null) { path.add(0, cursor); cursor = cursor.parent(); }
        for (int i = 0; i < path.size() - 1 && !event.isPropagationStopped(); i++) {
            UIComponent component = path.get(i);
            event.route(component, EventPhase.CAPTURE);
            component.dispatchEvent(event, EventPhase.CAPTURE);
        }
        if (!event.isPropagationStopped()) {
            event.route(event.target(), EventPhase.TARGET);
            event.target().dispatchEvent(event, EventPhase.TARGET);
        }
        for (int i = path.size() - 2; i >= 0 && !event.isPropagationStopped(); i--) {
            UIComponent component = path.get(i);
            event.route(component, EventPhase.BUBBLE);
            component.dispatchEvent(event, EventPhase.BUBBLE);
        }
    }

    private UIComponent inputTarget(double mouseX, double mouseY) {
        if (root != null && (layoutDirty || overlayLayoutDirty)) {
            if (layoutDirty) {
                root.layoutTree(font, x, y, width, height);
                layoutDirty = false;
            }
            if (overlayLayoutDirty) {
                overlays.layout(font, x, y, width, height);
                overlayLayoutDirty = false;
            }
        }
        UIComponent overlayTarget = overlays.hitTest((int) mouseX, (int) mouseY);
        return overlayTarget != null ? overlayTarget : root == null ? null : root.hitTest((int) mouseX, (int) mouseY);
    }

    /**
     * Walk target → parent bubbling mouseClicked() until a component returns true.
     * This ensures composite controls (Card, VirtualList, Table rows, etc.) whose
     * behaviour lives in mouseClicked() are not silently bypassed when the hit-test
     * returns a leaf descendant.
     */
    private boolean bubbleMouseClicked(UIComponent from, double mx, double my, int button) {
        UIComponent cursor = from;
        while (cursor != null) {
            if (cursor.mouseClicked(mx, my, button)) return true;
            cursor = cursor.parent();
        }
        return false;
    }

    /**
     * Walk target → parent bubbling mouseScrolled() until a component returns true.
     * This ensures scroll containers (VirtualList, ScrollPane) receive scroll events
     * when the pointer is over one of their children.
     */
    private boolean bubbleMouseScrolled(UIComponent from, double mx, double my, double delta) {
        UIComponent cursor = from;
        while (cursor != null) {
            if (cursor.mouseScrolled(mx, my, delta)) return true;
            cursor = cursor.parent();
        }
        return false;
    }

    @Override
    public void close() {
        nativeWidgets.close();
        animations.close();
        overlays.close();
        focus.clearFocus();
        pointerCapture = null;
        pressedTarget = null;
        if (root != null) root.dispose();
        root = null;
    }
}
