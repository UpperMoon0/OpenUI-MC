package com.nstut.openui.state;

import com.nstut.openui.form.Field;
import com.nstut.openui.form.Form;
import com.nstut.openui.navigation.Navigator;
import com.nstut.openui.navigation.Route;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ProductionStateTest {
    @Test void formValidityTracksFieldChanges() { Form form = Form.create(); Field<Integer> field = form.field(0).validate(v -> v > 0, "Must be positive"); assertFalse(form.isValid()); field.value().set(3); assertTrue(form.isValid()); assertNull(field.error().get()); }
    @Test void selectionSupportsModes() { SelectionModel<String> model = SelectionModel.multiple(); model.select("a"); model.toggle("b"); assertEquals(Set.of("a", "b"), model.selection().get()); }
    @Test void navigatorMaintainsHistory() { Navigator navigator = new Navigator(new Route<>("browse", 1)); navigator.push(new Route<>("orders", 2)); assertTrue(navigator.pop()); assertEquals("browse", navigator.current().get().id()); }
    @Test void stateStoreRemembersValues() { StateStore store = new StateStore(); Signal<Integer> first = store.remember("count", 1); assertSame(first, store.remember("count", 2)); }
}
