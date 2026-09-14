package com.nstut.openui.declarative;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeclarativeTreeTest {
    @Test
    void nestedKeyedReorderPreservesRetainedIdentity() {
        TestAdapter adapter = new TestAdapter();
        DeclarativeTree<Node> tree = new DeclarativeTree<>(adapter);
        Node root = new Node("root");

        tree.reconcile(root, List.of(
                node("column", "content", "column").withChildren(
                        node("text", "a", "a"),
                        node("text", "b", "b"))));

        Node column = root.children.get(0);
        Node a = column.children.get(0);
        Node b = column.children.get(1);

        tree.reconcile(root, List.of(
                node("column", "content", "unused-column").withChildren(
                        node("text", "b", "unused-b"),
                        node("text", "a", "unused-a"))));

        assertSame(column, root.children.get(0));
        assertEquals(List.of(b, a), column.children);
    }

    @Test
    void replacementDetachesBeforeReplacementMounts() {
        TestAdapter adapter = new TestAdapter();
        DeclarativeTree<Node> tree = new DeclarativeTree<>(adapter);
        Node root = new Node("root");

        tree.reconcile(root, List.of(node("text", "status", "old")));
        Node old = root.children.get(0);
        adapter.events.clear();

        tree.reconcile(root, List.of(node("button", "status", "replacement")));

        Node replacement = root.children.get(0);
        assertNotSame(old, replacement);
        assertEquals(List.of("detach:root->old", "attach:root->replacement"), adapter.events);
    }

    @Test
    void declarativeLeafMayEmbedLegacyRetainedSubtree() {
        TestAdapter adapter = new TestAdapter();
        DeclarativeTree<Node> tree = new DeclarativeTree<>(adapter);
        Node root = new Node("root");
        Node legacyChild = new Node("legacy-child");

        DeclarativeChild<Node> legacy = new DeclarativeChild<>(
                "legacy", "legacy", () -> {
                    Node parent = new Node("legacy-parent");
                    parent.children.add(legacyChild);
                    return parent;
                }, null);

        tree.reconcile(root, List.of(legacy));
        Node retained = root.children.get(0);
        assertEquals(List.of(legacyChild), retained.children);

        tree.reconcile(root, List.of(new DeclarativeChild<>(
                "legacy", "legacy", () -> new Node("unused"), value -> value.updates++)));
        assertSame(retained, root.children.get(0));
        assertEquals(List.of(legacyChild), retained.children);
        assertEquals(1, retained.updates);
    }

    @Test
    void nestedDeclarativeNodeMustStartWithEmptyRetainedChildren() {
        TestAdapter adapter = new TestAdapter();
        DeclarativeTree<Node> tree = new DeclarativeTree<>(adapter);
        Node root = new Node("root");
        DeclarativeChild<Node> invalid = new DeclarativeChild<>(
                "column", "content", () -> {
                    Node node = new Node("column");
                    node.children.add(new Node("imperative-child"));
                    return node;
                }, null, List.of(node("text", "child", "declarative-child")));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> tree.reconcile(root, List.of(invalid)));
        assertTrue(failure.getMessage().contains("must be created with no retained children"));
    }

    @Test
    void externalMutationOfOwnedChildrenFailsClosed() {
        TestAdapter adapter = new TestAdapter();
        DeclarativeTree<Node> tree = new DeclarativeTree<>(adapter);
        Node root = new Node("root");
        DeclarativeChild<Node> child = node("text", "a", "a");
        tree.reconcile(root, List.of(child));

        root.children.add(new Node("rogue"));
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> tree.reconcile(root, List.of(child)));
        assertTrue(failure.getMessage().contains("mutated outside the reconciler"));
    }

    private static DeclarativeChild<Node> node(String type, String key, String name) {
        return DeclarativeChild.of(type, key, () -> new Node(name));
    }

    private static final class Node {
        private final String name;
        private final List<Node> children = new ArrayList<>();
        private int updates;

        private Node(String name) { this.name = name; }

        @Override public String toString() { return name; }
    }

    private static final class TestAdapter implements DeclarativeTree.Adapter<Node> {
        private final List<String> events = new ArrayList<>();

        @Override public void attach(Node parent, Node child) {
            events.add("attach:" + parent + "->" + child);
            parent.children.add(child);
        }

        @Override public void detach(Node parent, Node child) {
            events.add("detach:" + parent + "->" + child);
            assertTrue(parent.children.remove(child));
        }

        @Override public List<Node> children(Node parent) { return parent.children; }

        @Override public void reorder(Node parent, List<Node> children) {
            parent.children.clear();
            parent.children.addAll(children);
        }
    }
}
