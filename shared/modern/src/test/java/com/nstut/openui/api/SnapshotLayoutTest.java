package com.nstut.openui.api;

import com.nstut.openui.layout.Alignment;
import com.nstut.openui.layout.Justification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotLayoutTest {

    @Test
    void snapshotCalculatesExpectedGeometryForNestedTree() {
        UIComponent sidebar = Ui.card(Ui.text("Sidebar")).width(80);
        UIComponent mainContent = Ui.padding(10, Ui.column(
                Ui.text("Header"),
                Ui.row(Ui.button("Action 1", () -> {}), Ui.button("Action 2", () -> {})).gap(8)
        )).flex();

        HStack root = Ui.row(sidebar, mainContent);
        root.layout(0, 0, 300, 200);

        // Sidebar bounds
        assertEquals(0, sidebar.getX());
        assertEquals(0, sidebar.getY());
        assertEquals(80, sidebar.getWidth());
        assertEquals(200, sidebar.getHeight());

        // Content bounds
        assertEquals(80, mainContent.getX());
        assertEquals(0, mainContent.getY());
        assertEquals(220, mainContent.getWidth());
        assertEquals(200, mainContent.getHeight());
    }

    @Test
    void stackCenterAlignmentCalculatesCorrectCenterCoordinates() {
        SizedBox child = new SizedBox(60, 40);
        Stack stack = Ui.stack(child).align(Alignment.CENTER, Alignment.CENTER);

        stack.layout(0, 0, 200, 100);

        // Center: x = (200 - 60) / 2 = 70, y = (100 - 40) / 2 = 30
        assertEquals(70, child.getX());
        assertEquals(30, child.getY());
        assertEquals(60, child.getWidth());
        assertEquals(40, child.getHeight());
    }
}
