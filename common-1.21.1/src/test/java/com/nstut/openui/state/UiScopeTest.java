package com.nstut.openui.state;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UiScopeTest {
    @Test
    void ownedResourcesCloseOnceInReverseOrder() {
        UiScope scope = new UiScope();
        StringBuilder order = new StringBuilder();
        scope.own(() -> order.append('a'));
        scope.own(() -> order.append('b'));

        scope.close();
        scope.close();

        assertEquals("ba", order.toString());
        assertTrue(scope.isClosed());
    }

    @Test
    void effectStopsWhenScopeCloses() {
        Signal<Integer> value = Signals.of(1);
        AtomicInteger observed = new AtomicInteger();
        UiScope scope = new UiScope();
        scope.effect(() -> observed.set(value.get()));

        assertEquals(1, observed.get());
        value.set(2);
        assertEquals(2, observed.get());

        scope.close();
        value.set(3);
        assertEquals(2, observed.get());
    }

    @Test
    void subscriptionStopsWhenScopeCloses() {
        Signal<Integer> value = Signals.of(0);
        AtomicInteger calls = new AtomicInteger();
        UiScope scope = new UiScope();
        scope.subscribe(value, ignored -> calls.incrementAndGet());

        value.set(1);
        assertEquals(1, calls.get());
        scope.close();
        value.set(2);
        assertEquals(1, calls.get());
    }

    @Test
    void rememberedStateIsStableForScopeLifetime() {
        UiScope scope = new UiScope();
        Signal<String> first = scope.remember("query", "one");
        first.set("changed");
        Signal<String> second = scope.remember("query", "two");

        assertSame(first, second);
        assertEquals("changed", second.get());
    }

    @Test
    void closedScopeRejectsNewOwnership() {
        UiScope scope = new UiScope();
        scope.close();
        assertThrows(IllegalStateException.class, () -> scope.remember("x", 1));
        assertThrows(IllegalStateException.class, () -> scope.own(() -> { }));
    }
}
