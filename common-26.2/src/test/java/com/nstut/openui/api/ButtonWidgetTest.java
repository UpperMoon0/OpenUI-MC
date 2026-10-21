package com.nstut.openui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ButtonWidgetTest {
    @Test
    void twoLineLabelsRequestEnoughVerticalSpace() {
        ButtonWidget button = new ButtonWidget("Grid", 0, 0, 0);
        assertEquals(14, button.preferredHeight(null));

        button.setLabel("Activity\nAll");
        assertEquals(22, button.preferredHeight(null));
    }

    @Test
    void ghostTextUsesTheHighestContrastThemeColor() {
        int darkText = 0xFF1C1C24;
        int lightText = 0xFFFFFFFF;
        assertEquals(darkText, ButtonWidget.contrastTextColor(0xFFDCDDE3, darkText, lightText));
        assertEquals(lightText, ButtonWidget.contrastTextColor(0xFF2A50A8, darkText, lightText));
        assertEquals(darkText, ButtonWidget.contrastTextColor(0x00000000, darkText, lightText));
    }
}
