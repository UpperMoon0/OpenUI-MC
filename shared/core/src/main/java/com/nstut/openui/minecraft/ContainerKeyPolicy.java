package com.nstut.openui.minecraft;

/** Shared routing policy for container-screen keyboard input across Minecraft versions. */
final class ContainerKeyPolicy {
    enum Route {
        VANILLA,
        OPEN_UI,
        OPEN_UI_THEN_VANILLA
    }

    private ContainerKeyPolicy() { }

    static Route route(boolean inventoryKey, boolean textInputFocused, boolean blockingOverlay) {
        if (!inventoryKey) return Route.OPEN_UI_THEN_VANILLA;
        return textInputFocused || blockingOverlay ? Route.OPEN_UI : Route.VANILLA;
    }
}
