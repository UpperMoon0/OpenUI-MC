package com.nstut.openui.minecraft;

import java.util.function.BooleanSupplier;

/** Shared keyboard ownership policy; native shortcuts must not bypass text or modals. */
final class ContainerKeyPolicy {
    enum Route { VANILLA, OPEN_UI, OPEN_UI_THEN_VANILLA }
    private ContainerKeyPolicy() { }

    static Route route(boolean inventoryKey, boolean textInputFocused, boolean blockingOverlay) {
        return route(-1, inventoryKey, textInputFocused, blockingOverlay);
    }

    static Route route(int key, boolean inventoryKey, boolean textInputFocused, boolean blockingOverlay) {
        // A non-dismissable modal owns Escape too. UiRuntime decides whether
        // an overlay may close; vanilla must never close the underlying screen.
        if (blockingOverlay) return Route.OPEN_UI;
        // Text arrives separately from key-down on modern clients. A native
        // EditBox returning false for Q/1 does not authorize THROW/SWAP below it.
        // Ordinary Escape may still close a screen when no modal owns it.
        if (textInputFocused && (inventoryKey || key != 256)) return Route.OPEN_UI;
        return inventoryKey ? Route.VANILLA : Route.OPEN_UI_THEN_VANILLA;
    }

    static boolean dispatch(int key, boolean inventoryKey, boolean textInputFocused, boolean blockingOverlay,
                            BooleanSupplier openUi, BooleanSupplier vanilla) {
        Route route = route(key, inventoryKey, textInputFocused, blockingOverlay);
        if (route == Route.VANILLA) return vanilla.getAsBoolean();
        if (openUi.getAsBoolean()) return true;
        return route == Route.OPEN_UI || vanilla.getAsBoolean();
    }
}
