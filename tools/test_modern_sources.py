import re
import subprocess
import sys
import shutil
import tempfile
import unittest
from pathlib import Path
import check_modern_sources as modern

class ModernSourceReuseTest(unittest.TestCase):
    def fixture(self,root):
        (root/'gradle').mkdir();shutil.copyfile(modern.ROOT/'gradle/modern-ui-sources.gradle',root/'gradle/modern-ui-sources.gradle')
        for module in modern.MODULES:
            (root/module).mkdir();(root/module/'build.gradle').write_text(modern.SHARED_SCRIPT+'\n')
        source=root/'shared/modern/src/main/java/example/Shared.java';source.parent.mkdir(parents=True);source.write_text('class Shared {}')
        return source
    def test_all_native_modules_use_one_authoritative_source_and_test_tree(self):
        for module in modern.MODULES:
            self.assertGreater(len(modern.inventory(modern.ROOT,module)),140)
            self.assertGreater(len(modern.inventory(modern.ROOT,module,'test')),30)
    def test_shared_edit_addition_and_deletion_are_visible_without_regeneration(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp);source=self.fixture(root);relative=Path('example/Shared.java')
            for content in ('class Shared { int first; }','class Shared { int second; }'):
                source.write_text(content)
                for module in modern.MODULES:self.assertEqual(content,modern.inventory(root,module)[relative].read_text())
            source.unlink()
            for module in modern.MODULES:self.assertNotIn(relative,modern.inventory(root,module))
    def test_stale_clone_or_native_shadow_fails_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp);source=self.fixture(root)
            clone=root/'common-26.2/src/main/java/Clone.java';clone.parent.mkdir(parents=True);clone.write_text('class Clone {}')
            with self.assertRaisesRegex(ValueError,'clone'):modern.inventory(root,modern.MODULES[0])
            clone.unlink();shadow=root/modern.MODULES[0]/'src/main/java/example/Shared.java';shadow.parent.mkdir(parents=True);shadow.write_text(source.read_text())
            with self.assertRaisesRegex(ValueError,'shadows'):modern.inventory(root,modern.MODULES[0])
    def test_all_native_screen_adapters_share_keyboard_ownership(self):
        for module in ('common-1.20.1','common-1.21.1','forge-1.16.5','forge-1.12.2','shared/modern'):
            for name in ('UiScreen.java','UiContainerScreen.java'):
                path=modern.ROOT/module/'src/main/java/com/nstut/openui/minecraft'/name
                self.assertIn('ContainerKeyPolicy.dispatch(',path.read_text(),str(path))
    def test_standalone_module_identity_uses_directory_not_display_name(self):
        script=(modern.ROOT/'gradle/modern-ui-sources.gradle').read_text()
        self.assertIn("'--module',project.projectDir.name",script)
        self.assertNotIn("'--module',project.name",script)
        for module in ('fabric-26.2','neoforge-26.2'):
            directory=modern.ROOT/module
            settings=(directory/'settings.gradle').read_text()
            display_name=re.search(r"rootProject.name\s*=\s*['\"]([^'\"]+)",settings).group(1)
            self.assertNotIn(display_name,modern.MODULES)
            result=subprocess.run([sys.executable,str(modern.ROOT/'tools/check_modern_sources.py'),'--module',directory.name],capture_output=True,text=True)
            self.assertEqual(0,result.returncode,result.stderr)
    def test_neoforge_262_icon_uses_current_loader_metadata(self):
        text=(modern.ROOT/'neoforge-26.2/src/main/resources/META-INF/neoforge.mods.toml').read_text()
        self.assertIn('iconFile="openui_mc.png"',text)
        self.assertNotIn('logoFile=',text)
    def test_disconnected_native_build_fails_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp);self.fixture(root);(root/modern.MODULES[0]/'build.gradle').write_text('// not wired\n')
            with self.assertRaisesRegex(ValueError,'authoritative'):modern.inventory(root,modern.MODULES[0])

if __name__=='__main__':unittest.main()
