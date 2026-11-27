"""Fail closed on missing/wrong legacy Forge pack metadata before client launch."""
import json
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
REQUIRED=('api/Ui','runtime/UiRuntime','state/Signal','controls/TextField','controls/Table','controls/VirtualList','minecraft/UiScreen','minecraft/UiContainerScreen')
class TargetArtifactTest(unittest.TestCase):
    def check(self,target,pack):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'fixture.jar'
            with zipfile.ZipFile(path,'w') as archive:
                for name in list(REQUIRED)+['fixture/Type'+str(i) for i in range(141)]:
                    archive.writestr('com/nstut/openui/'+name+'.class',bytes.fromhex('cafebabe00000034'))
                if target=='forge-1.12.2':
                    archive.writestr('mcmod.info',json.dumps([{'modid':'openui_mc','mcversion':'1.12.2'}]))
                else:archive.writestr('META-INF/mods.toml','[[mods]]\nmodId="openui_mc"\n')
                if pack is not None:archive.writestr('pack.mcmeta',json.dumps(pack))
            return subprocess.run([sys.executable,str(ROOT/'tools/check_target_artifact.py'),target,str(path)],capture_output=True,text=True)
    def test_both_native_forge_resource_packs_are_valid(self):
        for target in ('forge-1.12.2','forge-1.16.5'):
            with self.subTest(target=target):
                pack=json.loads((ROOT/target/'src/main/resources/pack.mcmeta').read_text())
                result=self.check(target,pack);self.assertEqual(0,result.returncode,result.stderr)
    def test_missing_wrong_or_empty_resource_metadata_is_rejected(self):
        for target in ('forge-1.12.2','forge-1.16.5'):
            for pack in (None,{'pack':{'pack_format':999,'description':'wrong'}},{'pack':{'pack_format':3 if target=='forge-1.12.2' else 6,'description':''}}):
                with self.subTest(target=target,pack=pack):self.assertNotEqual(0,self.check(target,pack).returncode)
    def test_each_forge_target_rejects_the_other_native_resource_pack(self):
        for target,other in (('forge-1.12.2','forge-1.16.5'),('forge-1.16.5','forge-1.12.2')):
            with self.subTest(target=target):
                pack=json.loads((ROOT/other/'src/main/resources/pack.mcmeta').read_text())
                result=self.check(target,pack)
                self.assertNotEqual(0,result.returncode)
                self.assertIn('incorrect native resource pack format',result.stderr)
    # Pipeline wiring is owned and tested in NsTut-CICD, not retired Actions YAML.
if __name__=='__main__':unittest.main()
