package com.nstut.openui.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayDepthTest {

    @Test
    void overlayStrideClearsMinecraftItemRenderDepth() {
        // GuiGraphics.renderItem() adds 150 Z internally on the supported Minecraft lines.
        assertTrue(OverlayManager.OVERLAY_Z_STRIDE > 150,
                "later overlays must render above native item depth from lower overlays");
    }
}
