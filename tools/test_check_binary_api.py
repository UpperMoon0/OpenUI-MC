import unittest
from check_binary_api import compatible_class_declaration as compatible


class ClassCompatibilityTest(unittest.TestCase):
    def test_added_interface(self):
        self.assertTrue(compatible('public final class A {', 'public final class A implements B {'))

    def test_existing_generic_interfaces_preserved(self):
        self.assertTrue(compatible('public class A implements Map<K, V> {',
                                   'public class A implements B, Map<K, V> {'))

    def test_removed_interface_rejected(self):
        self.assertFalse(compatible('public class A implements B {', 'public class A {'))

    def test_changed_superclass_rejected(self):
        self.assertFalse(compatible('public class A extends B {', 'public class A extends C implements D {'))

    def test_changed_modifiers_rejected(self):
        self.assertFalse(compatible('public class A {', 'public final class A implements B {'))

    def test_interface_declarations_are_still_strict(self):
        self.assertFalse(compatible('public interface A {', 'public interface A extends B {'))


if __name__ == '__main__':
    unittest.main()
