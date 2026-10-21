package com.nstut.openui.api;

import com.nstut.openui.component.DirtyFlag;
import com.nstut.openui.layout.Alignment;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Justification;
import com.nstut.openui.layout.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedLayoutTest {
    @Test
    void constraintsClampMeasuredSize() {
        UIComponent component = new Fixed(100, 5);
        assertEquals(new Size(40, 10), component.measure(new Constraints(10, 40, 10, 20), null));
    }

    @Test
    void rowCentersChildrenAndDistributesSpace() {
        UIComponent first = new Fixed(10, 4);
        UIComponent second = new Fixed(10, 4);
        HStack row = new HStack().align(Alignment.CENTER).justify(Justification.SPACE_BETWEEN).child(first).child(second);
        row.layoutTree(null, 0, 0, 100, 20);
        assertEquals(0, first.getX());
        assertEquals(90, second.getX());
        assertEquals(8, first.getY());
        assertEquals(4, first.getHeight());
    }

    @Test
    void textMutationInvalidatesLayout() {
        TextWidget text = TextWidget.label("a", 0);
        text.layoutTree(null, 0, 0, 10, 10);
        text.markPainted();
        assertFalse(text.isDirty(DirtyFlag.LAYOUT));
        text.setText("longer");
        assertTrue(text.isDirty(DirtyFlag.LAYOUT));
        assertTrue(text.isDirty(DirtyFlag.PAINT));
    }

    private static final class Fixed extends UIComponent {
        private final int width, height;
        private Fixed(int width, int height) { this.width = width; this.height = height; }
        @Override public int preferredWidth(Font font) { return width; }
        @Override public int preferredHeight(Font font) { return height; }
        @Override public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) { }
    }
}

