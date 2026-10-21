package com.nstut.openui.api;

import com.nstut.openui.controls.ContextMenu;
import com.nstut.openui.controls.Popover;
import com.nstut.openui.controls.Select;
import com.nstut.openui.controls.Tooltip;
import com.nstut.openui.state.Signals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OffsetViewportTest {

    @Test
    void popoverClampsWithinOffsetContainerViewport() {
        int vpX = 80, vpY = 40, vpW = 176, vpH = 166;

        UIComponent anchor = Ui.button("Anchor", () -> {});
        anchor.layout(vpX + 150, vpY + 140, 20, 20); // Near bottom-right of viewport

        UIComponent content = Ui.card(Ui.text("Popover Content")).width(60).height(40);
        Popover popover = Ui.popover(anchor, content);

        popover.layout(vpX, vpY, vpW, vpH);

        assertTrue(popover.getX() >= vpX, "Popover X (" + popover.getX() + ") must be >= viewport X (" + vpX + ")");
        assertTrue(popover.getX() + popover.getWidth() <= vpX + vpW, "Popover right edge must be <= viewport right");
        assertTrue(popover.getY() >= vpY, "Popover Y (" + popover.getY() + ") must be >= viewport Y (" + vpY + ")");
        assertTrue(popover.getY() + popover.getHeight() <= vpY + vpH, "Popover bottom edge must be <= viewport bottom");
    }

    @Test
    void tooltipClampsWithinOffsetContainerViewport() {
        int vpX = 80, vpY = 40, vpW = 176, vpH = 166;

        Tooltip tooltip = Ui.tooltip("Helpful description that is wide");
        tooltip.setPosition(vpX + 170, vpY + 160); // Far bottom-right

        tooltip.layout(vpX, vpY, vpW, vpH);

        assertTrue(tooltip.getX() >= vpX, "Tooltip X (" + tooltip.getX() + ") must be >= viewport X (" + vpX + ")");
        assertTrue(tooltip.getX() + tooltip.getWidth() <= vpX + vpW, "Tooltip right edge must be <= viewport right");
        assertTrue(tooltip.getY() >= vpY, "Tooltip Y (" + tooltip.getY() + ") must be >= viewport Y (" + vpY + ")");
        assertTrue(tooltip.getY() + tooltip.getHeight() <= vpY + vpH, "Tooltip bottom edge must be <= viewport bottom");
    }

    @Test
    void contextMenuClampsWithinOffsetContainerViewport() {
        int vpX = 80, vpY = 40, vpW = 176, vpH = 166;

        ContextMenu menu = Ui.contextMenu(vpX + 160, vpY + 150)
                .item("Cut", () -> {})
                .item("Copy", () -> {})
                .item("Paste", () -> {});

        menu.layout(vpX, vpY, vpW, vpH);

        assertTrue(menu.getX() >= vpX, "ContextMenu X (" + menu.getX() + ") must be >= viewport X (" + vpX + ")");
        assertTrue(menu.getX() + menu.getWidth() <= vpX + vpW, "ContextMenu right edge must be <= viewport right");
        assertTrue(menu.getY() >= vpY, "ContextMenu Y (" + menu.getY() + ") must be >= viewport Y (" + vpY + ")");
        assertTrue(menu.getY() + menu.getHeight() <= vpY + vpH, "ContextMenu bottom edge must be <= viewport bottom");
    }
}
