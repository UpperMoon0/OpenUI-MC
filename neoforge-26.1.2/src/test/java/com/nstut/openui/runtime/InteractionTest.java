package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.VStack;
import com.nstut.openui.controls.Card;
import com.nstut.openui.controls.Checkbox;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.controls.Select;
import com.nstut.openui.controls.SwitchControl;
import com.nstut.openui.input.EventType;
import com.nstut.openui.input.UiEvent;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InteractionTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void clippedTextFieldCannotReceiveVanillaFallbackClick() {
        Font font=new Font(null);
        List<AbstractWidget> mounted=new ArrayList<>();
        NativeWidgetHost host=new NativeWidgetHost() {
            @Override public void add(AbstractWidget widget) { mounted.add(widget); }
            @Override public void remove(AbstractWidget widget) { mounted.remove(widget); }
        };
        UiRuntime runtime=new UiRuntime(font,host);
        try {
            TextField field=new TextField(Signals.of(""),font);
            Card card=new Card(Ui.column(Ui.spacer().height(24),field))
                    .padding(0).radius(0).elevated(false);
            runtime.setViewport(0,0,100,30);
            runtime.setRoot(card);

            // Trigger the real runtime layout path without clicking the UI.
            assertFalse(runtime.mouseClicked(-10,-10,1));
            assertTrue(mounted.contains(field.getEditBox()),"TextField native EditBox must be mounted on the screen host");

            // Match EditBoxWrapper.render() native bounds so vanilla Screen fallback sees this pixel.
            var editBox=field.getEditBox();
            editBox.setX(field.getX()+4);
            editBox.setY(field.getY()+(field.getHeight()-font.lineHeight)/2);
            editBox.setWidth(Math.max(10,field.getWidth()-8));
            double clickX=field.getX()+5;
            double clippedY=card.getY()+card.getHeight()-1;

            java.util.function.BooleanSupplier vanillaWouldDispatchToEditBox=() ->
                    editBox.active&&editBox.visible
                            &&clickX>=editBox.getX()&&clickX<editBox.getX()+editBox.getWidth()
                            &&clippedY>=editBox.getY()&&clippedY<editBox.getY()+editBox.getHeight();
            assertTrue(vanillaWouldDispatchToEditBox.getAsBoolean(),
                    "control case: the native EditBox is eligible for Screen fallback at the clipped-away pixel");
            assertFalse(runtime.mouseClicked(clickX,clippedY,0),
                    "OpenUI hit-testing must reject the field outside Card's protected child clip");

            boolean vanillaOwnedResult=runtime.nativeWidgets().withMouseClickFallbackSuppressed(
                    vanillaWouldDispatchToEditBox);
            assertFalse(vanillaOwnedResult,
                    "screen fallback must make the OpenUI-owned native EditBox ineligible for direct dispatch");
            assertTrue(editBox.active,"native widget active state must be restored after fallback routing");

            AtomicBoolean unrelatedVanilla=new AtomicBoolean();
            assertTrue(runtime.nativeWidgets().withMouseClickFallbackSuppressed(() -> {
                unrelatedVanilla.set(true);
                return true;
            }),"unrelated vanilla fallback must remain available");
            assertTrue(unrelatedVanilla.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void fullyClippedTextFieldCannotKeepOrReceiveKeyboardFocus() {
        Font font=new Font(null);
        UiRuntime runtime=new UiRuntime(font,dummyHost);
        try {
            Signal<String> value=Signals.of("");
            TextField field=new TextField(value,font) {
                @Override public void onFocusGained() { }
                @Override public void onFocusLost() { }
                @Override public boolean charTyped(char character,int modifiers) {
                    value.set(value.get()+character);
                    return true;
                }
            };
            Card card=new Card(field).padding(0).radius(0).elevated(false);
            runtime.setViewport(0,0,100,30);
            runtime.setRoot(card);

            // Force real runtime layout, then establish a valid focus before moving the field out of the clip.
            assertFalse(runtime.mouseClicked(-10,-10,1));
            assertTrue(runtime.focus().requestFocus(field));
            assertSame(field,runtime.focus().focused());

            field.layout(5,40,80,18);
            assertFalse(field.hasVisibleAreaWithinAncestorClips(),
                    "field rectangle is fully removed by the Card child clip");

            assertFalse(runtime.charTyped('x',0),
                    "keyboard routing must clear focus rather than typing into a fully clipped field");
            assertEquals("",value.get());
            assertNull(runtime.focus().focused());
            assertFalse(runtime.focus().requestFocus(field),
                    "programmatic focus must reject a fully clipped descendant");
            assertFalse(runtime.keyPressed(258,0,0),
                    "Tab traversal must skip the only fully clipped focusable descendant");
            assertNull(runtime.focus().focused());
        } finally {
            runtime.close();
        }
    }

    @Test
    void simulatedButtonClickDispatchesAction() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        AtomicBoolean clicked = new AtomicBoolean(false);

        ButtonWidget btn = Ui.button("Test", () -> clicked.set(true));
        runtime.setRoot(btn);
        runtime.setViewport(0, 0, 200, 100);

        runtime.mouseClicked(10, 10, 0);
        runtime.mouseReleased(10, 10, 0);

        assertTrue(clicked.get());
    }

    @Test
    void checkboxTogglesStateOnSimulatedClick() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        Signal<Boolean> checked = Signals.of(false);

        Checkbox cb = Ui.checkbox("Check", checked);
        runtime.setRoot(cb);
        runtime.setViewport(0, 0, 200, 100);

        assertFalse(checked.get());

        runtime.mouseClicked(5, 5, 0);
        assertTrue(checked.get());

        runtime.mouseClicked(5, 5, 0);
        assertFalse(checked.get());
    }

    @Test
    void selectDropdownKeyboardArrowNavigation() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        Signal<String> selection = Signals.of("A");

        Select<String> select = Ui.select(selection)
                .option("A", "A")
                .option("B", "B")
                .option("C", "C");

        runtime.setRoot(select);
        runtime.focus().requestFocus(select);

        assertEquals("A", selection.get());

        runtime.keyPressed(264, 0, 0);
        assertEquals("B", selection.get());

        runtime.keyPressed(264, 0, 0);
        assertEquals("C", selection.get());

        runtime.keyPressed(265, 0, 0);
        assertEquals("B", selection.get());
    }

    @Test
    void selectDropdownStaysOpenWhenOpeningPressIsReleased() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);
        Signal<String> selection = Signals.of("A");
        Select<String> select = Ui.select(selection).option("A", "A").option("B", "B");
        VStack root = new VStack();
        root.addChild(select);
        runtime.setRoot(root);
        runtime.setViewport(0, 0, 120, 80);

        runtime.mouseClicked(10, 10, 0);
        assertEquals(1, runtime.overlays().size());

        runtime.mouseReleased(10, 10, 0);
        assertEquals(1, runtime.overlays().size());

        runtime.mouseClicked(110, 70, 0);
        assertEquals(0, runtime.overlays().size());
        runtime.close();
    }

    @Test
    void clickOnLeafGivesFocusToNearestFocusableAncestor() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);

        AtomicReference<UIComponent> focused = new AtomicReference<>();
        UIComponent card = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 100; }
            @Override public int preferredHeight(Font font) { return 40; }
            @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {}
            @Override public boolean mouseClicked(double mx, double my, int btn) {
                focused.set(this);
                return true;
            }
        };
        card.focusable(true);
        UIComponent text = Ui.text("Leaf");
        card.addChild(text);

        runtime.setRoot(card);
        runtime.setViewport(0, 0, 200, 100);
        card.layout(0, 0, 100, 40);
        text.layout(10, 10, 80, 20);

        runtime.mouseClicked(50, 20, 0);
        assertEquals(card, runtime.focus().focused(), "Card should receive focus when its leaf child is clicked");
        assertEquals(card, focused.get(), "Card mouseClicked should have been invoked via bubbling");
    }

    @Test
    void stopPropagationPreventsLegacyBubbleHandlers() {
        UiRuntime runtime = new UiRuntime(new Font(null), dummyHost);

        AtomicReference<UIComponent> ancestorClicked = new AtomicReference<>();
        AtomicBoolean leafListenerRan = new AtomicBoolean();

        UIComponent ancestor = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 100; }
            @Override public int preferredHeight(Font font) { return 40; }
            @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {}
            @Override public boolean mouseClicked(double mx, double my, int btn) {
                ancestorClicked.set(this);
                return true;
            }
        };

        UIComponent leaf = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 50; }
            @Override public int preferredHeight(Font font) { return 20; }
            @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {}
            @Override public boolean mouseClicked(double mx, double my, int btn) {
                fail("Leaf default handler must not run after stopPropagation");
                return false;
            }
        };
        leaf.on(EventType.MOUSE_DOWN, event -> {
            leafListenerRan.set(true);
            event.stopPropagation();
        });

        ancestor.addChild(leaf);
        runtime.setRoot(ancestor);
        runtime.setViewport(0, 0, 200, 100);
        ancestor.layout(0, 0, 100, 40);
        leaf.layout(10, 10, 50, 20);

        runtime.mouseClicked(30, 20, 0);
        assertTrue(leafListenerRan.get(), "Target listener should execute before stopping propagation");
        assertNull(ancestorClicked.get(), "Ancestor legacy mouseClicked should be suppressed by stopPropagation");
    }
}
