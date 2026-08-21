package com.nstut.openui.navigation;

public record Route<T>(String id, T data) {
    public static Route<Void> of(String id) { return new Route<>(id, null); }
}

