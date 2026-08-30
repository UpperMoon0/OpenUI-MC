package com.nstut.openui.minecraft;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContainerKeyPolicyTest {
    @Test
    void sendsUnfocusedInventoryKeyDirectlyToVanilla() {
        assertEquals(ContainerKeyPolicy.Route.VANILLA,
                ContainerKeyPolicy.route(true, false, false));
    }

    @Test
    void keepsInventoryKeyInsideTextInputsAndBlockingOverlays() {
        assertEquals(ContainerKeyPolicy.Route.OPEN_UI,
                ContainerKeyPolicy.route(true, true, false));
        assertEquals(ContainerKeyPolicy.Route.OPEN_UI,
                ContainerKeyPolicy.route(true, false, true));
    }

    @Test
    void dispatchesOrdinaryKeysThroughOpenUiBeforeVanilla() {
        assertEquals(ContainerKeyPolicy.Route.OPEN_UI_THEN_VANILLA,
                ContainerKeyPolicy.route(false, false, false));
    }
}
