package com.nstut.openui.form;

@FunctionalInterface
public interface Validator<T> {
    String validate(T value);

    static <T> Validator<T> of(java.util.function.Predicate<T> predicate, String message) {
        return value -> predicate.test(value) ? null : message;
    }
}

