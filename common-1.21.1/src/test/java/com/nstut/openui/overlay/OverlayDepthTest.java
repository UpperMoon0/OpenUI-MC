package com.nstut.openui.overlay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayDepthTest {

    @Test
    void renderPlanesKeepLaterOverlaysAboveNativeItemsFromEarlierPlanes() {
        List<String> overlays = List.of("modal", "popover", "tooltip");
        List<String> renderedOrder = new ArrayList<>();
        List<Integer> renderedDepths = new ArrayList<>();

        OverlayManager.renderPlanes(overlays, (overlay, z) -> {
            renderedOrder.add(overlay);
            renderedDepths.add(z);
        });

        assertEquals(overlays, renderedOrder, "overlay render order must be preserved");
        assertEquals(overlays.size(), renderedDepths.size());

        // GuiGraphics.renderItem() adds 150 Z internally on the supported legacy lines.
        for (int i = 0; i < renderedDepths.size() - 1; i++) {
            int lowerNativeItemTop = renderedDepths.get(i) + 150;
            int nextOverlayPlane = renderedDepths.get(i + 1);
            assertTrue(lowerNativeItemTop < nextOverlayPlane,
                    "native item depth from overlay " + i + " must stay below overlay " + (i + 1));
        }
    }
}
