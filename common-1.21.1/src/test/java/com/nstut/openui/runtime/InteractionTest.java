package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.Checkbox;
import com.nstut.openui.controls.Select;
import com.nstut.openui.controls.SwitchControl;
import com.nstut.openui.input.EventType;
import com.nstut.openui.input.UiEvent;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InteractionTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    @Test
    void simulatedButtonClickDispatchesAction() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
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
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
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
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);
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
    void clickOnLeafGivesFocusToNearestFocusableAncestor() {
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);

        AtomicReference<UIComponent> focused = new AtomicReference<>();
        UIComponent card = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 100; }
            @Override public int preferredHeight(Font font) { return 40; }
            @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {}
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
        UiRuntime runtime = new UiRuntime(new Font(null, false), dummyHost);

        AtomicReference<UIComponent> ancestorClicked = new AtomicReference<>();
        AtomicReference<UIComponent> leafClicked = new AtomicReference<>();

        UIComponent ancestor = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 100; }
            @Override public int preferredHeight(Font font) { return 40; }
            @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {}
            @Override public boolean mouseClicked(double mx, double my, int btn) {
                ancestorClicked.set(this);
                return true;
            }
        };

        UIComponent leaf = new UIComponent() {
            @Override public int preferredWidth(Font font) { return 50; }
            @Override public int preferredHeight(Font font) { return 20; }
            @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) {}
            @Override public boolean mouseClicked(double mx, double my, int btn) {
                leafClicked.set(this);
                return true;
            }
        };
        leaf.onCapture(EventType.MOUSE_DOWN, event -> event.stopPropagation());

        ancestor.addChild(leaf);
        runtime.setRoot(ancestor);
        runtime.setViewport(0, 0, 200, 100);
        ancestor.layout(0, 0, 100, 40);
        leaf.layout(10, 10, 50, 20);

        runtime.mouseClicked(30, 20, 0);
        assertEquals(leaf, leafClicked.get(), "Leaf should still receive its legacy mouseClicked");
        assertNull(ancestorClicked.get(), "Ancestor legacy mouseClicked should be suppressed by stopPropagation");
    }
}
