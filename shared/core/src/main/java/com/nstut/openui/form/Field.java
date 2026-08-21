package com.nstut.openui.form;

import com.nstut.openui.state.Computed;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;

import java.util.ArrayList;
import java.util.List;

public final class Field<T> {
    private final Signal<T> value;
    private final Signal<Integer> validatorVersion = Signals.of(0);
    private final List<Validator<T>> validators = new ArrayList<>();
    private final Computed<String> error = Signals.computed(this::computeError);

    Field(T initialValue) { value = Signals.of(initialValue); }
    public Signal<T> value() { return value; }
    public Computed<String> error() { return error; }
    public boolean isValid() { return error.get() == null; }
    public Field<T> validate(Validator<T> validator) { validators.add(validator); validatorVersion.update(v -> v + 1); return this; }
    public Field<T> validate(java.util.function.Predicate<T> predicate, String message) { return validate(Validator.of(predicate, message)); }

    private String computeError() {
        validatorVersion.get();
        T current = value.get();
        for (Validator<T> validator : validators) {
            String error = validator.validate(current);
            if (error != null) return error;
        }
        return null;
    }
}
