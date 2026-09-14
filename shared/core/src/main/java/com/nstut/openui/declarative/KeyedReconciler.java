package com.nstut.openui.declarative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small, runtime-independent child matching algorithm for declarative OpenUI.
 *
 * <p>Keyed siblings preserve identity across reorders. Unkeyed siblings are
 * positional and only reuse the old node at the same index when its type also
 * matches. The class intentionally performs no tree mutation; runtime adapters
 * apply the returned plan to retained components.</p>
 */
public final class KeyedReconciler {
    private KeyedReconciler() { }

    public static ReconcilePlan plan(List<NodeIdentity> oldNodes, List<NodeIdentity> newNodes) {
        List<NodeIdentity> oldCopy = List.copyOf(oldNodes);
        List<NodeIdentity> newCopy = List.copyOf(newNodes);
        validateUniqueKeys(oldCopy, "old");
        validateUniqueKeys(newCopy, "new");

        Map<String, Integer> keyedOld = new HashMap<>();
        for (int i = 0; i < oldCopy.size(); i++) {
            NodeIdentity identity = oldCopy.get(i);
            if (identity.key() != null) keyedOld.put(identity.key(), i);
        }

        boolean[] claimed = new boolean[oldCopy.size()];
        List<Integer> oldIndexForNew = new ArrayList<>(newCopy.size());

        for (int newIndex = 0; newIndex < newCopy.size(); newIndex++) {
            NodeIdentity next = newCopy.get(newIndex);
            int matched = -1;

            if (next.key() != null) {
                Integer candidateIndex = keyedOld.get(next.key());
                if (candidateIndex != null && !claimed[candidateIndex]) {
                    NodeIdentity candidate = oldCopy.get(candidateIndex);
                    if (candidate.type().equals(next.type())) matched = candidateIndex;
                }
            } else if (newIndex < oldCopy.size() && !claimed[newIndex]) {
                NodeIdentity candidate = oldCopy.get(newIndex);
                if (candidate.key() == null && candidate.type().equals(next.type())) matched = newIndex;
            }

            oldIndexForNew.add(matched);
            if (matched >= 0) claimed[matched] = true;
        }

        List<Integer> removed = new ArrayList<>();
        for (int oldIndex = 0; oldIndex < oldCopy.size(); oldIndex++) {
            if (!claimed[oldIndex]) removed.add(oldIndex);
        }
        return new ReconcilePlan(oldIndexForNew, removed);
    }

    private static void validateUniqueKeys(List<NodeIdentity> nodes, String side) {
        Set<String> keys = new HashSet<>();
        for (NodeIdentity node : nodes) {
            String key = node.key();
            if (key != null && !keys.add(key)) {
                throw new IllegalArgumentException("Duplicate " + side + " sibling key: " + key);
            }
        }
    }
}
