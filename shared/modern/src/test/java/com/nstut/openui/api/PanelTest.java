package com.nstut.openui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PanelTest {
    @Test
    void constructorsPreserveThemeAndExplicitColorSemantics() {
        Panel defaults = new Panel();
        assertEquals(defaults.theme().colors().surfaceRaised(), defaults.effectiveBackground());

        Panel backgroundOnly = new Panel(0xFF123456);
        assertEquals(0xFF123456, backgroundOnly.effectiveBackground());
        assertEquals(0, backgroundOnly.effectiveBorder());

        Panel elevatedBackground = new Panel(0xFF123456).elevated();
        assertEquals(elevatedBackground.theme().colors().border(), elevatedBackground.effectiveBorder());

        Panel explicit = new Panel(0, 0);
        assertEquals(0, explicit.effectiveBackground());
        assertEquals(0, explicit.effectiveBorder());
    }
}
