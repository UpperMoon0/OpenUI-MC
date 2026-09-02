package com.nstut.openui.declarative;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyedReconcilerTest {
    private static NodeIdentity node(String type, String key) {
        return new NodeIdentity(type, key);
    }

    @Test
    void keyedReorderPreservesOldIdentity() {
        ReconcilePlan plan = KeyedReconciler.plan(
                List.of(node("row", "a"), node("row", "b"), node("row", "c")),
                List.of(node("row", "c"), node("row", "a"), node("row", "b")));

        assertEquals(List.of(2, 0, 1), plan.oldIndexForNew());
        assertTrue(plan.removedOldIndices().isEmpty());
    }

    @Test
    void sameKeyDifferentTypeReplacesNode() {
        ReconcilePlan plan = KeyedReconciler.plan(
                List.of(node("text", "status")),
                List.of(node("button", "status")));

        assertFalse(plan.reuses(0));
        assertEquals(List.of(0), plan.removedOldIndices());
    }

    @Test
    void unkeyedNodesUsePositionalSemantics() {
        ReconcilePlan plan = KeyedReconciler.plan(
                List.of(node("text", null), node("button", null)),
                List.of(node("text", null), node("slider", null)));

        assertTrue(plan.reuses(0));
        assertEquals(0, plan.oldIndex(0));
        assertFalse(plan.reuses(1));
        assertEquals(List.of(1), plan.removedOldIndices());
    }

    @Test
    void removedKeyedNodeIsReportedForUnmount() {
        ReconcilePlan plan = KeyedReconciler.plan(
                List.of(node("row", "a"), node("row", "b")),
                List.of(node("row", "b")));

        assertEquals(List.of(1), plan.oldIndexForNew());
        assertEquals(List.of(0), plan.removedOldIndices());
    }

    @Test
    void duplicateSiblingKeysAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> KeyedReconciler.plan(
                List.of(),
                List.of(node("text", "same"), node("button", "same"))));
    }
}
