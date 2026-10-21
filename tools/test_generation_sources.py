import tempfile
import unittest
from pathlib import Path
import fabric_1165_sources
import forge_1122_sources

class NativeSourceGenerationTest(unittest.TestCase):
    def test_fabric_names_are_official_without_changing_behavior(self):
        text=fabric_1165_sources.convert('import net.minecraft.client.gui.FontRenderer; import com.mojang.blaze3d.matrix.MatrixStack; FontRenderer f; MatrixStack p;')
        self.assertIn('net.minecraft.client.gui.Font;',text)
        self.assertIn('com.mojang.blaze3d.vertex.PoseStack;',text)
        self.assertIn('Font f; PoseStack p;',text)
    def test_1122_uses_native_text_types_and_java8_api(self):
        text=forge_1122_sources.convert('import net.minecraft.network.chat.Component; Component text=Component.literal("hello"); String s=text.getString(); var xs=List.of(1);')
        self.assertIn('net.minecraft.util.text.TextComponentString("hello")',text)
        self.assertIn('text.getUnformattedText()',text)
        self.assertNotIn('List.of',text)
    def test_generated_outputs_are_deterministic_and_remove_stale_classes(self):
        for module in [fabric_1165_sources,forge_1122_sources]:
            with self.subTest(module=module.__name__),tempfile.TemporaryDirectory() as temp:
                output=Path(temp);stale=output/'Stale.java';stale.write_text('class Stale {}')
                module.generate(output)
                self.assertFalse(stale.exists())
                before={p.relative_to(output):p.read_bytes() for p in output.rglob('*.java')}
                self.assertGreater(len(before),140)
                module.generate(output)
                self.assertEqual(before,{p.relative_to(output):p.read_bytes() for p in output.rglob('*.java')})
    def test_1122_keys_remain_framework_keys(self):
        text=forge_1122_sources.convert('public boolean keyPressed(int key,int scan,int modifiers) { return key==258 || key==256; }')
        self.assertIn('key==258 || key==256',text)

if __name__=='__main__':unittest.main()
