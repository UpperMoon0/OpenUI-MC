package com.nstut.openui.minecraft;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.compat.LegacyFont;
import com.nstut.openui.compat.LegacyWidget;
import com.nstut.openui.compat.LegacyTextField;
import com.nstut.openui.graphics.LegacyGuiGraphics;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.util.LinkedHashSet;
import java.util.Set;
public abstract class UiContainerScreen<T extends Container> extends GuiContainer {
    protected final ITextComponent title;
    private UiRuntime uiRuntime;
    private final Set<LegacyWidget> widgets=new LinkedHashSet<>();
    private double lastMouseX,lastMouseY;
    protected UiContainerScreen(T menu,InventoryPlayer inventory,ITextComponent title) { super(menu);this.title=title; }
    protected abstract UIComponent buildUI();
    protected int uiLeft() { return guiLeft; } protected int uiTop() { return guiTop; }
    protected int uiWidth() { return xSize; } protected int uiHeight() { return ySize; }
    public final UiRuntime uiRuntime() { return uiRuntime; }
    @Override public void initGui() {
        super.initGui();
        if(uiRuntime!=null)uiRuntime.close();
        uiRuntime=new UiRuntime(new LegacyFont(fontRenderer),new NativeWidgetHost() {
            public void add(LegacyWidget widget) { widgets.add(widget); }
            public void remove(LegacyWidget widget) { widgets.remove(widget); }
        });
        uiRuntime.setViewport(uiLeft(),uiTop(),uiWidth(),uiHeight());uiRuntime.setRoot(buildUI());
    }
    protected final void rebuildUI() { if(uiRuntime!=null)uiRuntime.setRoot(buildUI()); }
    @Override public void drawScreen(int x,int y,float partialTick) {
        super.drawScreen(x,y,partialTick);
        try(LegacyGuiGraphics graphics=new LegacyGuiGraphics()) {
            if(uiRuntime!=null)uiRuntime.render(graphics,x,y,partialTick);
            renderForegroundLayer(graphics,partialTick,x,y);
        }
        if(uiRuntime==null || !uiRuntime.overlays().suppressesUnderlyingTooltips(x,y))renderHoveredToolTip(x,y);
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partialTick,int x,int y) {
        try(LegacyGuiGraphics graphics=new LegacyGuiGraphics()) { renderBackgroundLayer(graphics,partialTick,x,y); }
    }
    protected void renderBackgroundLayer(LegacyGuiGraphics graphics,float partialTick,int x,int y) { }
    protected void renderForegroundLayer(LegacyGuiGraphics graphics,float partialTick,int x,int y) { }
    @Override public void updateScreen() { super.updateScreen();for(LegacyWidget widget:widgets)if(widget instanceof LegacyTextField)((LegacyTextField)widget).tick(); }
    @Override protected void mouseClicked(int x,int y,int button) {
        lastMouseX=x;lastMouseY=y;
        if(uiRuntime==null || !uiRuntime.mouseClicked(x,y,button))nativeFallback(() -> super.mouseClicked(x,y,button));
    }
    @Override protected void mouseReleased(int x,int y,int button) { if(uiRuntime==null || !uiRuntime.mouseReleased(x,y,button))super.mouseReleased(x,y,button); }
    @Override protected void mouseClickMove(int x,int y,int button,long elapsed) {
        double dx=x-lastMouseX,dy=y-lastMouseY;lastMouseX=x;lastMouseY=y;
        if(uiRuntime==null || !uiRuntime.mouseDragged(x,y,button,dx,dy))super.mouseClickMove(x,y,button,elapsed);
    }
    @Override public void handleMouseInput() {
        int delta=Mouse.getEventDWheel();double x=Mouse.getEventX()*width/(double)mc.displayWidth,y=height-Mouse.getEventY()*height/(double)mc.displayHeight-1;
        if(delta!=0 && uiRuntime!=null)uiRuntime.mouseScrolled(x,y,Math.signum(delta));
        nativeFallback(() -> super.handleMouseInput());
    }
    @Override protected void keyTyped(char character,int scanCode) {
        int key=LegacyKeys.translate(scanCode,character),modifiers=LegacyKeys.modifiers();
        ContainerKeyPolicy.dispatch(key, mc != null && scanCode==mc.gameSettings.keyBindInventory.getKeyCode(),
                uiRuntime!=null&&uiRuntime.hasTextInputFocus(),uiRuntime!=null&&uiRuntime.overlays().hasBlockingOverlay(),
                () -> uiRuntime!=null && (uiRuntime.keyPressed(key,scanCode,modifiers)
                        || (character>=32 && uiRuntime.charTyped(character,modifiers))),
                () -> { nativeFallback(() -> super.keyTyped(character,scanCode));return true; });
    }
    @Override public void handleKeyboardInput() {
        if(!Keyboard.getEventKeyState() && uiRuntime!=null)uiRuntime.keyReleased(LegacyKeys.translate(Keyboard.getEventKey(),Keyboard.getEventCharacter()),Keyboard.getEventKey(),LegacyKeys.modifiers());
        nativeFallback(() -> super.handleKeyboardInput());
    }
    @Override public void onGuiClosed() { if(uiRuntime!=null)uiRuntime.close();uiRuntime=null;widgets.clear();super.onGuiClosed(); }
    private interface NativeFallback { void run() throws Exception; }
    private static void nativeFallback(NativeFallback action) {
        try { action.run(); } catch(RuntimeException error) { throw error; }
        catch(Exception error) { throw new IllegalStateException("Native GUI event failed",error); }
    }
}
