package com.nstut.openui.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiContextTest {
    @Test
    void nearestProviderWins() {
        ContextKey<String> key = ContextKey.create("name");
        UiContext root = UiContext.empty().with(key, "root");
        UiContext child = root.with(key, "child");

        assertEquals("child", child.require(key));
        assertEquals("root", root.require(key));
    }

    @Test
    void keysUseIdentityEvenWithSameDebugName() {
        ContextKey<String> first = ContextKey.create("service");
        ContextKey<String> second = ContextKey.create("service");
        UiContext context = UiContext.empty().with(first, "value");

        assertEquals("value", context.require(first));
        assertFalse(context.find(second).isPresent());
    }

    @Test
    void requiredMissingContextHasUsefulFailure() {
        ContextKey<String> key = ContextKey.create("client");
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> UiContext.empty().require(key));
        assertEquals("Missing OpenUI context value for client", failure.getMessage());
    }
}
