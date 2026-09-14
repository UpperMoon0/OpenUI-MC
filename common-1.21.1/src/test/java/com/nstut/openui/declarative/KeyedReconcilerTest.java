package com.nstut.openui.declarative;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        assertEquals(-1, plan.oldIndex(0));
        assertEquals(List.of(0), plan.removedOldIndices());
    }

    @Test
    void unkeyedNodesUseStrictPositionalSemantics() {
        ReconcilePlan plan = KeyedReconciler.plan(
                List.of(node("text", null), node("button", null)),
                List.of(node("text", null), node("slider", null)));

        assertTrue(plan.reuses(0));
        assertEquals(0, plan.oldIndex(0));
        assertFalse(plan.reuses(1));
        assertEquals(List.of(1), plan.removedOldIndices());
    }

    @Test
    void insertedKeyedNodeDoesNotStealUnkeyedPositionalIdentity() {
        ReconcilePlan plan = KeyedReconciler.plan(
                List.of(node("text", null), node("text", "kept")),
                List.of(node("text", "new"), node("text", "kept")));

        assertEquals(List.of(-1, 1), plan.oldIndexForNew());
        assertEquals(List.of(0), plan.removedOldIndices());
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
    void duplicateSiblingKeysAreRejectedOnBothSides() {
        IllegalArgumentException oldDuplicate = assertThrows(IllegalArgumentException.class, () ->
                KeyedReconciler.plan(
                        List.of(node("text", "same"), node("button", "same")),
                        List.of()));
        IllegalArgumentException newDuplicate = assertThrows(IllegalArgumentException.class, () ->
                KeyedReconciler.plan(
                        List.of(),
                        List.of(node("text", "same"), node("button", "same"))));

        assertTrue(oldDuplicate.getMessage().contains("Duplicate old sibling key"));
        assertTrue(newDuplicate.getMessage().contains("Duplicate new sibling key"));
    }
}
