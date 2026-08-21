package com.nstut.openui.form;

import com.nstut.openui.state.Computed;
import com.nstut.openui.state.Signals;

import java.util.ArrayList;
import java.util.List;

public final class Form {
    private final List<Field<?>> fields = new ArrayList<>();
    private final Computed<Boolean> valid = Signals.computed(() -> fields.stream().allMatch(Field::isValid));

    public static Form create() { return new Form(); }
    public <T> Field<T> field(T initialValue) { Field<T> field = new Field<>(initialValue); fields.add(field); return field; }
    public Computed<Boolean> valid() { return valid; }
    public boolean isValid() { return valid.get(); }
}
