#!/usr/bin/env python3
"""Derive Java 8 Fabric/Mojang-name sources from the tested legacy adapters.

Forge 1.16.5 uses MCP class/package identities with Mojang member mappings;
Fabric uses the full official names. Keep the behavioral adaptation in the
existing legacy generator, and translate only those class identities here.
"""
import argparse
import re
from pathlib import Path
import legacy_sources
ROOT = Path(__file__).resolve().parents[1]
PACKAGES = {
    'com.mojang.blaze3d.matrix.MatrixStack': 'com.mojang.blaze3d.vertex.PoseStack',
    'net.minecraft.util.math.vector.Vector4f': 'com.mojang.math.Vector4f',
    'net.minecraft.client.gui.AbstractGui': 'net.minecraft.client.gui.GuiComponent',
    'net.minecraft.client.gui.FontRenderer': 'net.minecraft.client.gui.Font',
    'net.minecraft.client.gui.widget.Widget': 'net.minecraft.client.gui.components.AbstractWidget',
    'net.minecraft.client.gui.widget.TextFieldWidget': 'net.minecraft.client.gui.components.EditBox',
    'net.minecraft.client.gui.screen.inventory.ContainerScreen': 'net.minecraft.client.gui.screens.inventory.AbstractContainerScreen',
    'net.minecraft.client.gui.screen.Screen': 'net.minecraft.client.gui.screens.Screen',
    'net.minecraft.client.audio.SimpleSound': 'net.minecraft.client.resources.sounds.SimpleSoundInstance',
    'net.minecraft.util.text.StringTextComponent': 'net.minecraft.network.chat.TextComponent',
    'net.minecraft.util.text.TranslationTextComponent': 'net.minecraft.network.chat.TranslatableComponent',
    'net.minecraft.util.text.ITextComponent': 'net.minecraft.network.chat.Component',
    'net.minecraft.util.text.ITextProperties': 'net.minecraft.network.chat.FormattedText',
    'net.minecraft.util.text.Style': 'net.minecraft.network.chat.Style',
    'net.minecraft.util.IReorderingProcessor': 'net.minecraft.util.FormattedCharSequence',
    'net.minecraft.util.ResourceLocation': 'net.minecraft.resources.ResourceLocation',
    'net.minecraft.util.SoundEvents': 'net.minecraft.sounds.SoundEvents',
    'net.minecraft.item.ItemStack': 'net.minecraft.world.item.ItemStack',
    'net.minecraft.entity.player.PlayerInventory': 'net.minecraft.world.entity.player.Inventory',
    'net.minecraft.inventory.container.Container': 'net.minecraft.world.inventory.AbstractContainerMenu',
    'net.minecraft.util.Util': 'net.minecraft.Util',
}
NAMES = {a.rsplit('.', 1)[1]:b.rsplit('.', 1)[1] for a,b in PACKAGES.items()}
def convert(text):
    for old,new in PACKAGES.items(): text=text.replace(old,new)
    for old,new in NAMES.items():
        if old != new: text=re.sub(r'\b'+old+r'\b',new,text)
    return text

def generate(output):
    generated=set()
    for source in [ROOT/'shared/core/src/main/java',ROOT/'common-1.20.1/src/main/java',ROOT/'forge-1.16.5/src/main/java']:
        for path in source.rglob('*.java'):
            relative=path.relative_to(source)
            if 'forge' in relative.parts: continue
            is_adapter='forge-1.16.5' in source.parts
            if not is_adapter and relative.name in ['UiScreen.java','UiContainerScreen.java']: continue
            text=path.read_text(encoding='utf-8')
            if not is_adapter: text=legacy_sources.convert(text)
            target=output/relative;target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(convert(text),encoding='utf-8');generated.add(target)
    for obsolete in output.rglob('*.java'):
        if obsolete not in generated: obsolete.unlink()
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True,type=Path)
    generate(parser.parse_args().output)
