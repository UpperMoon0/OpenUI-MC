package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreRenderDispatchTest {
    private static class ComponentProbe extends UIComponent {
        int calls;

        @Override public int preferredWidth(Font font) { return 100; }
        @Override public int preferredHeight(Font font) { return 100; }
        @Override public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) { }

        @Override public void preRender(int mouseX, int mouseY) {
            calls++;
            super.preRender(mouseX, mouseY);
        }
    }

    private static final class ClipProbe extends ComponentProbe {
        @Override protected boolean clipsChildrenToBounds() { return true; }
    }

    private static final class ThrowingProbe extends ComponentProbe {
        boolean failNext;

        @Override public void preRender(int mouseX, int mouseY) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("test exception");
            }
            super.preRender(mouseX, mouseY);
        }
    }

    @Test
    void subclassPreRenderRunsOnEveryFrameWithoutLosingAncestorClipEligibility() {
        ClipProbe outer = new ClipProbe();
        ClipProbe inner = new ClipProbe();
        ComponentProbe leaf = new ComponentProbe();
        outer.addChild(inner);
        inner.addChild(leaf);

        outer.layoutTree(null, 0, 0, 50, 50);
        inner.layoutTree(null, 0, 0, 100, 100);
        leaf.layoutTree(null, 0, 0, 100, 100);

        outer.preRender(20, 20);
        assertEquals(1, inner.calls, "the public override must receive the frame");
        assertEquals(1, leaf.calls, "nested public overrides must also run");
        assertTrue(leaf.isHovered());

        outer.preRender(75, 20);
        assertEquals(2, inner.calls, "subclass logic still runs outside clipping");
        assertEquals(2, leaf.calls, "descendant logic still runs outside clipping");
        assertFalse(inner.isHovered(), "parent clip suppresses hover");
        assertFalse(leaf.isHovered(), "parent clip propagates through nested descendants");

        outer.preRender(20, 20);
        assertEquals(3, leaf.calls);
        assertTrue(leaf.isHovered(), "hover recovers on re-entry");
    }

    @Test
    void exceptionalOverrideDoesNotLeakClippingToIndependentCalls() {
        ClipProbe outer = new ClipProbe();
        ThrowingProbe child = new ThrowingProbe();
        outer.addChild(child);
        outer.layoutTree(null, 0, 0, 50, 50);
        child.layoutTree(null, 0, 0, 100, 100);

        child.failNext = true;
        assertThrows(IllegalStateException.class, () -> outer.preRender(75, 20));

        // Direct calls must not inherit transient clip eligibility, even when
        // the preceding parent traversal terminated abnormally.
        child.preRender(75, 20);
        assertTrue(child.isHovered());
        assertEquals(1, child.calls);
    }
}
