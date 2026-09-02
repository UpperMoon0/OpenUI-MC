package com.nstut.openui.graphics;

import java.util.Objects;

/** Renderer-neutral namespaced texture identifier. */
public record UiTexture(String id) {
    public UiTexture {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("texture id cannot be blank");
    }

    public static UiTexture of(String namespace, String path) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        return new UiTexture(namespace + ":" + path);
    }

    public static UiTexture minecraft(String path) {
        return of("minecraft", path);
    }
}
