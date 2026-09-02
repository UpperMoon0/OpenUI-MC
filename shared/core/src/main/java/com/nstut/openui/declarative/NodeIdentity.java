package com.nstut.openui.declarative;

import java.util.Objects;

/** Identity used by the small declarative reconciler. */
public record NodeIdentity(Object type, String key) {
    public NodeIdentity {
        Objects.requireNonNull(type, "type");
    }

    /** Keyed nodes match by type + key; unkeyed nodes are positional. */
    public boolean matches(NodeIdentity other) {
        return other != null && type.equals(other.type) && Objects.equals(key, other.key);
    }
}
