package com.nstut.openui.declarative;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runtime-independent adapter that reconciles declarative descriptions onto a
 * retained tree.
 *
 * <p>Only direct children described by a non-empty declarative child list are
 * owned by this adapter. Leaf descriptions therefore remain safe wrappers for
 * legacy retained components that manage their own internal children.</p>
 */
public final class DeclarativeTree<T> {
    public interface Adapter<T> {
        void attach(T parent, T child);
        void detach(T parent, T child);
        List<T> children(T parent);
        void reorder(T parent, List<T> children);
    }

    private final Adapter<T> adapter;
    private List<MountedNode<T>> roots = List.of();

    public DeclarativeTree(Adapter<T> adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    /** Reconciles the complete declaratively-owned child list of {@code root}. */
    public void reconcile(T root, List<DeclarativeChild<T>> descriptions) {
        Objects.requireNonNull(root, "root");
        List<DeclarativeChild<T>> next = descriptions == null ? List.of() : List.copyOf(descriptions);
        roots = reconcileChildren(root, roots, next, true);
    }

    public List<T> roots() {
        return roots.stream().map(MountedNode::component).toList();
    }

    public void reset() {
        roots = List.of();
    }

    private List<MountedNode<T>> reconcileChildren(
            T parent,
            List<MountedNode<T>> oldNodes,
            List<DeclarativeChild<T>> nextDescriptions,
            boolean ownsDirectChildren) {
        if (ownsDirectChildren) verifyOwnership(parent, oldNodes);

        List<NodeIdentity> oldIds = oldNodes.stream().map(node -> node.description().identity()).toList();
        List<NodeIdentity> newIds = nextDescriptions.stream().map(DeclarativeChild::identity).toList();
        ReconcilePlan plan = KeyedReconciler.plan(oldIds, newIds);

        List<T> prepared = new ArrayList<>(nextDescriptions.size());
        for (int newIndex = 0; newIndex < nextDescriptions.size(); newIndex++) {
            DeclarativeChild<T> description = nextDescriptions.get(newIndex);
            int oldIndex = plan.oldIndex(newIndex);
            if (oldIndex >= 0) {
                T reused = oldNodes.get(oldIndex).component();
                description.apply(reused);
                prepared.add(reused);
            } else {
                T created = description.create();
                if (!description.children().isEmpty() && !adapter.children(created).isEmpty()) {
                    throw new IllegalStateException(
                            "Declarative node '" + description.type() + "' with child descriptions must be created with no retained children");
                }
                prepared.add(created);
            }
        }

        // Replacements/removals unmount before any replacement is attached.
        for (int oldIndex : plan.removedOldIndices()) {
            adapter.detach(parent, oldNodes.get(oldIndex).component());
        }

        List<MountedNode<T>> nextNodes = new ArrayList<>(nextDescriptions.size());
        for (int newIndex = 0; newIndex < nextDescriptions.size(); newIndex++) {
            DeclarativeChild<T> description = nextDescriptions.get(newIndex);
            int oldIndex = plan.oldIndex(newIndex);
            T component = prepared.get(newIndex);
            List<MountedNode<T>> childNodes;

            if (oldIndex >= 0) {
                MountedNode<T> oldNode = oldNodes.get(oldIndex);
                boolean ownsChildren = !oldNode.children().isEmpty() || !description.children().isEmpty();
                childNodes = ownsChildren
                        ? reconcileChildren(component, oldNode.children(), description.children(), true)
                        : List.of();
            } else {
                adapter.attach(parent, component);
                childNodes = description.children().isEmpty()
                        ? List.of()
                        : reconcileChildren(component, List.of(), description.children(), true);
            }

            nextNodes.add(new MountedNode<>(description, component, childNodes));
        }

        if (ownsDirectChildren) {
            adapter.reorder(parent, nextNodes.stream().map(MountedNode::component).toList());
        }
        return List.copyOf(nextNodes);
    }

    private void verifyOwnership(T parent, List<MountedNode<T>> oldNodes) {
        List<T> expected = oldNodes.stream().map(MountedNode::component).toList();
        List<T> actual = List.copyOf(adapter.children(parent));
        if (!actual.equals(expected)) {
            throw new IllegalStateException(
                    "Declaratively-owned retained children were mutated outside the reconciler; expected "
                            + expected.size() + " managed children but found " + actual.size());
        }
    }

    private record MountedNode<T>(
            DeclarativeChild<T> description,
            T component,
            List<MountedNode<T>> children) {
        private MountedNode {
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(component, "component");
            children = List.copyOf(children);
        }
    }
}
