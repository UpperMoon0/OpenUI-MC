import unittest
import tempfile
from pathlib import Path
from unittest.mock import patch
import legacy_sources
from legacy_sources import convert, end_pair
class LegacySourcesTest(unittest.TestCase):
    def test_regeneration_removes_deleted_and_renamed_sources(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'common-1.20.1/src/main/java/example'
            source.mkdir(parents=True)
            old = source / 'Removed.java'
            old.write_text('package example; public class Removed {}')
            output = root / 'generated'
            with patch.object(legacy_sources, 'ROOT', root), patch('sys.argv', ['legacy_sources.py', '--output', str(output)]):
                legacy_sources.main()
                self.assertTrue((output / 'example/Removed.java').exists())
                old.unlink()
                (source / 'Renamed.java').write_text('package example; public class Renamed {}')
                legacy_sources.main()
                self.assertFalse((output / 'example/Removed.java').exists())
                self.assertTrue((output / 'example/Renamed.java').exists())
    def test_nested_methods_do_not_suppress_outer_record_contract(self):
        text=convert('public record Outer(int value) { public record Inner(int x) { public boolean equals(Object other) { return false; } } }')
        self.assertIn('Outer __v=(Outer)__other',text)
        self.assertIn('this.value=value',text)
        self.assertIn('static final class Inner',text)
    def test_compact_constructor_validation_precedes_assignment(self):
        text=convert('public record Count(int value) { public Count { if(value<0) throw new IllegalArgumentException(); value=Math.min(10,value); } }')
        self.assertLess(text.index('Math.min'),text.index('this.value=value'))
    def test_comments_and_strings_do_not_change_balanced_braces(self):
        text='{ String value="}"; /* } */ // }\n } suffix'
        self.assertEqual(text.index(' suffix')-1,end_pair(text,0,'{','}'))
    def test_api_rewrites_do_not_touch_modern_sources(self):
        text=convert('List.copyOf(values); " ".repeat(2); value.isBlank();')
        self.assertIn('Java8.listCopy(values)',text)
        self.assertIn('Java8.repeat(" ",2)',text)
        self.assertIn('Java8.isBlank(value)',text)
if __name__=='__main__': unittest.main()
