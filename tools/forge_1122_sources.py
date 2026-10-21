#!/usr/bin/env python3
"""Derive the full shared OpenUI runtime for native Minecraft 1.12.2 adapters."""
import argparse,re
from pathlib import Path
import legacy_sources
ROOT=Path(__file__).resolve().parents[1]
def convert(text):
    text=legacy_sources.convert(text)
    mappings={
        'net.minecraft.client.gui.FontRenderer':'com.nstut.openui.compat.LegacyFont',
        'net.minecraft.client.gui.widget.TextFieldWidget':'com.nstut.openui.compat.LegacyTextField',
        'net.minecraft.client.gui.widget.Widget':'com.nstut.openui.compat.LegacyWidget',
        'net.minecraft.util.IReorderingProcessor':'com.nstut.openui.compat.LegacyOrderedText',
        'net.minecraft.util.text.StringTextComponent':'net.minecraft.util.text.TextComponentString',
        'net.minecraft.util.text.TranslationTextComponent':'net.minecraft.util.text.TextComponentTranslation',
        'com.mojang.blaze3d.systems.RenderSystem':'net.minecraft.client.renderer.GlStateManager',
    }
    for a,b in mappings.items():text=text.replace(a,b)
    for a,b in [('FontRenderer','LegacyFont'),('TextFieldWidget','LegacyTextField'),('Widget','LegacyWidget'),('IReorderingProcessor','LegacyOrderedText'),('RenderSystem','GlStateManager')]:text=re.sub(r'\b'+a+r'\b',b,text)
    text=text.replace('import net.minecraft.util.text.ITextProperties;\n','').replace('import net.minecraft.util.Util;\n','')
    text=text.replace('Util.getMillis()', 'System.nanoTime() / 1_000_000L')
    text=text.replace('GlStateManager.color4f(', 'GlStateManager.color(')
    text=text.replace('net.minecraft.util.text.Style.EMPTY','new net.minecraft.util.text.Style()')
    text=re.sub(r'\b(\w+)\.getVisualOrderText\(\)',r'com.nstut.openui.compat.LegacyOrderedText.from(\1)',text)
    text=text.replace('.getString()', '.getUnformattedText()')
    text=text.replace('Minecraft.getInstance().font', 'com.nstut.openui.compat.LegacyFont.current()').replace('Minecraft.getInstance()', 'Minecraft.getMinecraft()')
    text=text.replace('ResourceLocation.tryParse(', 'com.nstut.openui.compat.LegacyResources.tryParse(')
    text=text.replace('.append(col.header()).append(sortIndicator)', '.appendSibling(col.header()).appendText(sortIndicator)')
    return text

def generate(output):
    generated=set()
    for source in [ROOT/'shared/core/src/main/java',ROOT/'common-1.20.1/src/main/java']:
        for path in source.rglob('*.java'):
            relative=path.relative_to(source)
            if relative.name in ['UiScreen.java','UiContainerScreen.java','UiPlatform.java']:continue
            target=output/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(convert(path.read_text()),encoding='utf-8');generated.add(target)
    for obsolete in output.rglob('*.java'):
        if obsolete not in generated:obsolete.unlink()
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);generate(p.parse_args().output)
