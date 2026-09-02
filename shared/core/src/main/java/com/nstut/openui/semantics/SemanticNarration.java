package com.nstut.openui.semantics;

import com.nstut.openui.api.SemanticComponent;
import com.nstut.openui.api.UIComponent;

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

    /** Resolves the nearest semantic wrapper for a retained component/focus target. */
    public static String describeNearest(UIComponent component) {
        UIComponent cursor = component;
        while (cursor != null) {
            if (cursor instanceof SemanticComponent semantic) return describe(semantic.semantics());
            cursor = cursor.parent();
        }
        return "";
    }

    /** Collects visible semantic descriptions in retained-tree order. */
    public static List<String> describeTree(UIComponent root) {
        if (root == null) return List.of();
        List<String> result = new ArrayList<>();
        collect(root, result);
        return List.copyOf(result);
    }

    private static void collect(UIComponent component, List<String> result) {
        if (!component.isVisible()) return;
        if (component instanceof SemanticComponent semantic) {
            String description = describe(semantic.semantics());
            if (!description.isBlank()) result.add(description);
        }
        for (UIComponent child : component.children()) collect(child, result);
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
