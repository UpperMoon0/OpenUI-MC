package com.nstut.openui.api;

import com.nstut.openui.context.ContextKey;
import com.nstut.openui.semantics.SemanticNarration;
import com.nstut.openui.semantics.Semantics;
import com.nstut.openui.state.Effect;
import com.nstut.openui.state.Signals;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModernFrameworkApiTest {
    @Test
    void contextProviderUpdatesReactiveConsumers() {
        ContextKey<String> key = ContextKey.create("service");
        FixedComponent child = new FixedComponent(10, 5);
        ContextProvider provider = Ui.provide(key, "one", child);
        AtomicReference<String> observed = new AtomicReference<>();

        try (Effect ignored = Signals.effect(() -> observed.set(Contexts.require(child, key)))) {
            assertEquals("one", observed.get());
            provider.value(key, "two");
            assertEquals("two", observed.get());
        }
    }

    @Test
    void semanticNarrationResolvesFromFocusedLeafToWrapper() {
        FixedComponent child = new FixedComponent(10, 5);
        SemanticComponent semantic = Ui.semantic(
                Semantics.button().label("Buy item").build(), child);

        assertSame(semantic, child.parent());
        assertEquals("Buy item, button", SemanticNarration.describeNearest(child));
        assertEquals(List.of("Buy item, button"), SemanticNarration.describeTree(semantic));
    }

    @Test
    void styledBoxAppliesExplicitSizeConstraintsAndPadding() {
        FixedComponent child = new FixedComponent(10, 5);
        Style style = Style.builder()
                .padding(2)
                .margin(1)
                .width(30)
                .height(20)
                .build();
        StyledBox box = Ui.styled(StateStyle.of(style), child);

        assertEquals(32, box.preferredWidth(null));
        assertEquals(22, box.preferredHeight(null));
        box.layout(4, 6, 100, 100);

        assertEquals(32, box.getWidth());
        assertEquals(22, box.getHeight());
        assertEquals(7, child.getX());
        assertEquals(9, child.getY());
        assertEquals(26, child.getWidth());
        assertEquals(16, child.getHeight());
    }

    private static final class FixedComponent extends UIComponent {
        private final int preferredWidth;
        private final int preferredHeight;

        private FixedComponent(int preferredWidth, int preferredHeight) {
            this.preferredWidth = preferredWidth;
            this.preferredHeight = preferredHeight;
        }

        @Override public int preferredWidth(Font font) { return preferredWidth; }
        @Override public int preferredHeight(Font font) { return preferredHeight; }
        @Override public void render(GuiGraphics g, Font font, int mx, int my, float pt) { }
    }
}
