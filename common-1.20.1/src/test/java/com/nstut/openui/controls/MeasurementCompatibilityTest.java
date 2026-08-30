package com.nstut.openui.controls;

import com.nstut.openui.api.VStack;
import com.nstut.openui.layout.Constraints;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeasurementCompatibilityTest {

    @Test
    void subclassPreferredHeightOverridesRemainAuthoritative() {
        Font font = new Font(null, false);
        Card card = new Card() {
            @Override public int preferredHeight(Font ignored) { return 137; }
        };
        VStack stack = new VStack() {
            @Override public int preferredHeight(Font ignored) { return 149; }
        };

        assertEquals(137, card.measure(Constraints.loose(200, 200), font).height());
        assertEquals(149, stack.measure(Constraints.loose(200, 200), font).height());
    }
}
