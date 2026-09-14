package com.nstut.openui.semantics;

import java.util.List;
import java.util.Objects;

/** Renderer-independent semantic description for narration and navigation. */
public record Semantics(
        Role role,
        String label,
        String value,
        boolean disabled,
        boolean selected,
        boolean checked,
        List<Action> actions) {

    public Semantics {
        role = Objects.requireNonNullElse(role, Role.GENERIC);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static Builder builder(Role role) { return new Builder(role); }
    public static Builder button() { return builder(Role.BUTTON); }
    public static Builder checkbox() { return builder(Role.CHECKBOX); }
    public static Builder slider() { return builder(Role.SLIDER); }
    public static Builder heading() { return builder(Role.HEADING); }

    public enum Role {
        GENERIC, GROUP, TEXT, HEADING, BUTTON, CHECKBOX, RADIO, SWITCH, SLIDER,
        TEXT_FIELD, TAB, TAB_LIST, LIST, LIST_ITEM, TABLE, ROW, CELL, IMAGE, LINK
    }

    public record Action(String name, Runnable invoke) {
        public Action {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(invoke, "invoke");
        }
    }

    public static final class Builder {
        private final Role role;
        private String label;
        private String value;
        private boolean disabled;
        private boolean selected;
        private boolean checked;
        private List<Action> actions = List.of();

        private Builder(Role role) { this.role = Objects.requireNonNull(role, "role"); }
        public Builder label(String value) { label = value; return this; }
        public Builder value(String value) { this.value = value; return this; }
        public Builder disabled(boolean value) { disabled = value; return this; }
        public Builder selected(boolean value) { selected = value; return this; }
        public Builder checked(boolean value) { checked = value; return this; }
        public Builder actions(List<Action> value) { actions = List.copyOf(value); return this; }
        public Semantics build() { return new Semantics(role, label, value, disabled, selected, checked, actions); }
    }
}
