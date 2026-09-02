package com.nstut.openui.semantics;

import java.util.ArrayList;
import java.util.List;

/** Converts renderer-independent semantics into concise Minecraft narration text. */
public final class SemanticNarration {
    private SemanticNarration() { }

    public static String describe(Semantics semantics) {
        if (semantics == null) return "";
        List<String> parts = new ArrayList<>();
        if (semantics.label() != null && !semantics.label().isBlank()) parts.add(semantics.label());
        if (semantics.value() != null && !semantics.value().isBlank()) parts.add(semantics.value());
        String role = roleName(semantics.role());
        if (!role.isEmpty()) parts.add(role);
        if (semantics.checked()) parts.add("checked");
        if (semantics.selected()) parts.add("selected");
        if (semantics.disabled()) parts.add("disabled");
        return String.join(", ", parts);
    }

    private static String roleName(Semantics.Role role) {
        return switch (role) {
            case GENERIC, GROUP, TEXT -> "";
            case HEADING -> "heading";
            case BUTTON -> "button";
            case CHECKBOX -> "checkbox";
            case RADIO -> "radio button";
            case SWITCH -> "switch";
            case SLIDER -> "slider";
            case TEXT_FIELD -> "text field";
            case TAB -> "tab";
            case TAB_LIST -> "tab list";
            case LIST -> "list";
            case LIST_ITEM -> "list item";
            case TABLE -> "table";
            case ROW -> "row";
            case CELL -> "cell";
            case IMAGE -> "image";
            case LINK -> "link";
        };
    }
}
