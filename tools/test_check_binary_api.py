import pathlib
import shutil
import subprocess
import tempfile
import types
import unittest
import sys
from unittest.mock import patch

from check_binary_api import (
    check_baseline,
    class_api,
    compatible_class_declaration as compatible,
    parse_args,
    parse_class_api,
)


class ArgumentParsingTest(unittest.TestCase):
    def test_repeated_baseline_urls_preserve_order(self):
        argv = [
            'check_binary_api.py',
            '--baseline-url', 'https://example.invalid/v0.0.9.jar',
            '--baseline-url', 'https://example.invalid/v0.0.7.jar',
            '--candidate', 'candidate.jar',
            '--report', 'report.txt',
        ]
        with patch.object(sys, 'argv', argv):
            args = parse_args()
        self.assertEqual(
            ['https://example.invalid/v0.0.9.jar', 'https://example.invalid/v0.0.7.jar'],
            args.baseline_urls)


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

    def test_protected_method_removal_is_visible_to_comparison(self):
        baseline = """public class A {
  protected void hook();
    descriptor: ()V
}
"""
        candidate = """public class A {
}
"""
        _, _, baseline_members = parse_class_api(baseline)
        _, _, candidate_members = parse_class_api(candidate)
        self.assertEqual({('protected void hook();', '()V')}, baseline_members - candidate_members)

    def test_protected_field_descriptor_change_is_visible_to_comparison(self):
        baseline = """public class A {
  protected int state;
    descriptor: I
}
"""
        candidate = """public class A {
  protected long state;
    descriptor: J
}
"""
        _, _, baseline_members = parse_class_api(baseline)
        _, _, candidate_members = parse_class_api(candidate)
        self.assertIn(('protected int state;', 'I'), baseline_members - candidate_members)

    @unittest.skipUnless(shutil.which('javac') and shutil.which('jar') and shutil.which('javap'), 'JDK tools required')
    def test_protected_member_break_is_detected_from_real_classfiles(self):
        with tempfile.TemporaryDirectory() as temp:
            root = pathlib.Path(temp)
            baseline_src = root / 'baseline-src'
            candidate_src = root / 'candidate-src'
            baseline_classes = root / 'baseline-classes'
            candidate_classes = root / 'candidate-classes'
            for directory in (baseline_src, candidate_src, baseline_classes, candidate_classes):
                directory.mkdir()
            (baseline_src / 'A.java').write_text(
                'public class A { protected int state; protected void hook() {} }', encoding='utf-8')
            (candidate_src / 'A.java').write_text(
                'public class A { protected long state; }', encoding='utf-8')
            subprocess.run([shutil.which('javac'), '-d', baseline_classes, baseline_src / 'A.java'], check=True)
            subprocess.run([shutil.which('javac'), '-d', candidate_classes, candidate_src / 'A.java'], check=True)
            baseline_jar = root / 'baseline.jar'
            candidate_jar = root / 'candidate.jar'
            subprocess.run([shutil.which('jar'), 'cf', baseline_jar, '-C', baseline_classes, '.'], check=True)
            subprocess.run([shutil.which('jar'), 'cf', candidate_jar, '-C', candidate_classes, '.'], check=True)

            checked_classes, checked_members, failures = check_baseline(
                shutil.which('javap'), baseline_jar, candidate_jar)
            self.assertEqual(1, checked_classes)
            self.assertEqual(3, checked_members)
            self.assertTrue(any('protected int state' in failure for failure in failures))
            self.assertTrue(any('protected void hook' in failure for failure in failures))

    @patch('check_binary_api.subprocess.run')
    def test_javap_requests_protected_members(self, run):
        run.return_value = types.SimpleNamespace(
            returncode=0, stdout='public class A {\n}\n', stderr='')
        class_api('javap', pathlib.Path('candidate.jar'), 'A')
        command = run.call_args.args[0]
        self.assertIn('-protected', command)
        self.assertNotIn('-public', command)


if __name__ == '__main__':
    unittest.main()
