package com.nstut.openui.state;

import com.nstut.openui.form.Field;
import com.nstut.openui.form.Form;
import com.nstut.openui.navigation.Navigator;
import com.nstut.openui.navigation.Route;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductionStateTest {
    @Test
    void formValidityTracksFieldChanges() {
        Form form = Form.create();
        Field<Integer> quantity = form.field(0).validate(value -> value > 0, "Must be positive");

        assertFalse(form.isValid());
        assertEquals("Must be positive", quantity.error().get());
        quantity.value().set(3);
        assertTrue(form.isValid());
        assertNull(quantity.error().get());
    }

    @Test
    void selectionSupportsSingleAndMultipleModes() {
        SelectionModel<String> single = SelectionModel.single();
        single.select("a");
        single.select("b");
        assertEquals(Set.of("b"), single.selection().get());

        SelectionModel<String> multiple = SelectionModel.multiple();
        multiple.select("a");
        multiple.toggle("b");
        assertEquals(Set.of("a", "b"), multiple.selection().get());
        multiple.toggle("a");
        assertEquals(Set.of("b"), multiple.selection().get());
    }

    @Test
    void navigatorMaintainsAHistoryStack() {
        Navigator navigator = new Navigator(new Route<>("browse", 1));
        navigator.push(new Route<>("orders", 2));
        assertEquals("orders", navigator.current().get().id());
        assertTrue(navigator.pop());
        assertEquals("browse", navigator.current().get().id());
        assertFalse(navigator.pop());
    }

    @Test
    void stateStoreRemembersValuesByKey() {
        StateStore store = new StateStore();
        Signal<Integer> first = store.remember("count", 1);
        first.set(8);
        assertSame(first, store.remember("count", 99));
        assertEquals(8, store.<Integer>remember("count", 99).get());
    }
}
