package com.nstut.openui.api;

import com.nstut.openui.component.DirtyFlag;
import com.nstut.openui.context.ContextKey;
import com.nstut.openui.semantics.SemanticNarration;
import com.nstut.openui.semantics.Semantics;
import com.nstut.openui.state.AsyncValue;
import com.nstut.openui.state.Effect;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import com.nstut.openui.state.UiScope;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModernFrameworkApiTest {
    private static final Executor DIRECT = Runnable::run;

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
    void buildScopeSharesRememberedStateAndInheritedContextForOneMount() {
        ContextKey<String> key = ContextKey.create("service");
        FixedComponent child = new FixedComponent(10, 5);
        Ui.provide(key, "inherited", child);
        UiScope lifecycle = new UiScope();
        UiBuildScope build = new UiBuildScope(lifecycle, child);

        Signal<Integer> first = build.remember("count", 1);
        first.set(7);
        Signal<Integer> second = build.remember("count", 99);

        assertSame(first, second);
        assertEquals(7, second.get());
        assertEquals("inherited", build.context(key));
        assertEquals("inherited", build.findContext(key).orElseThrow());
        lifecycle.close();
    }

    @Test
    void buildScopeAsyncIsKeyedAndPublishesSuccessWithoutDuplicateWork() {
        UiScope lifecycle = new UiScope();
        UiBuildScope build = new UiBuildScope(lifecycle, new FixedComponent(1, 1));
        AtomicInteger workCalls = new AtomicInteger();

        ReadableSignal<AsyncValue<Integer>> first = build.async(
                "profile", workCalls::incrementAndGet, DIRECT, DIRECT);
        ReadableSignal<AsyncValue<Integer>> second = build.async(
                "profile", workCalls::incrementAndGet, DIRECT, DIRECT);

        assertSame(first, second);
        assertEquals(1, workCalls.get());
        assertEquals(AsyncValue.Status.SUCCESS, first.get().status());
        assertEquals(1, first.get().value());
        lifecycle.close();
    }

    @Test
    void buildScopeAsyncPublishesOriginalFailure() {
        UiScope lifecycle = new UiScope();
        UiBuildScope build = new UiBuildScope(lifecycle, new FixedComponent(1, 1));
        IllegalStateException expected = new IllegalStateException("boom");

        ReadableSignal<AsyncValue<Integer>> state = build.async(
                "failing",
                () -> { throw expected; },
                DIRECT,
                DIRECT);

        assertEquals(AsyncValue.Status.ERROR, state.get().status());
        assertSame(expected, state.get().error());
        lifecycle.close();
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

    @Test
    void styledBoxStateTransitionsInvalidateLayoutForLayoutAffectingVariants() {
        FixedComponent child = new FixedComponent(10, 5);
        StateStyle styles = new StateStyle(
                Style.EMPTY,
                Style.builder().padding(4).build(),
                Style.builder().width(40).build(),
                Style.builder().margin(3).build(),
                Style.builder().height(30).build());
        StyledBox box = Ui.styled(styles, child);

        box.layoutTree(null, 0, 0, 100, 100);
        assertFalse(box.isDirty(DirtyFlag.LAYOUT));

        box.pressed(true);
        assertTrue(box.isDirty(DirtyFlag.LAYOUT));
        box.layoutTree(null, 0, 0, 100, 100);

        box.disabled(true);
        assertTrue(box.isDirty(DirtyFlag.LAYOUT));
        box.layoutTree(null, 0, 0, 100, 100);

        box.onHoverEnter();
        assertTrue(box.isDirty(DirtyFlag.LAYOUT));
        box.layoutTree(null, 0, 0, 100, 100);

        box.onFocusGained();
        assertTrue(box.isDirty(DirtyFlag.LAYOUT));
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
