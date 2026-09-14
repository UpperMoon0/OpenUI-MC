package com.nstut.openui.declarative;

import java.util.List;

/**
 * Pure reconciliation result. An old index of -1 means the new node must be
 * mounted; removed old indices must be unmounted.
 */
public record ReconcilePlan(List<Integer> oldIndexForNew, List<Integer> removedOldIndices) {
    public ReconcilePlan {
        oldIndexForNew = List.copyOf(oldIndexForNew);
        removedOldIndices = List.copyOf(removedOldIndices);
    }

    public boolean reuses(int newIndex) {
        return oldIndexForNew.get(newIndex) >= 0;
    }

    public int oldIndex(int newIndex) {
        return oldIndexForNew.get(newIndex);
    }
}
