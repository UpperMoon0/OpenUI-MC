"""Exercise the real JavaCompiler driver across deletion/rename, without Jabel or Minecraft."""
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JAVA=Path(os.environ.get('JAVA17_HOME',os.environ.get('JAVA_HOME_17_X64','')))/'bin/java'
if not JAVA.is_file(): JAVA=Path(shutil.which('java') or '/missing-java')

class CompilerOutputTest(unittest.TestCase):
    def test_rename_and_delete_remove_class_files_but_keep_resources(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);out=root/'classes';out.mkdir();resource=out/'mcmod.info';resource.write_text('keep')
            sources=root/'sources.args';old=root/'Old.java';old.write_text('class Old { static class Nested {} }')
            def compile(paths):
                sources.write_text('\n'.join(str(path) for path in paths))
                subprocess.run([str(JAVA),str(ROOT/'tools/CompileLegacy.java'),str(sources),'--release','8','-d',str(out)],check=True,capture_output=True,text=True)
            compile([old]);self.assertTrue((out/'Old.class').exists());self.assertTrue((out/'Old$Nested.class').exists())
            old.unlink();new=root/'New.java';new.write_text('class New {}');compile([new])
            self.assertFalse((out/'Old.class').exists());self.assertFalse((out/'Old$Nested.class').exists());self.assertTrue((out/'New.class').exists())
            new.unlink();compile([]);self.assertFalse(list(out.rglob('*.class')));self.assertEqual('keep',resource.read_text())

if __name__=='__main__':unittest.main()
