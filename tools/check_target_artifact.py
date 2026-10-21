#!/usr/bin/env python3
"""Verify complete native artifacts, exact metadata and Java class compatibility."""
import argparse
import json
import struct
import zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('target');p.add_argument('jar',type=Path);a=p.parse_args()
loader,version=a.target.split('-',1);major=52 if version.startswith('1.') else 69
required=['api/Ui','runtime/UiRuntime','state/Signal','controls/TextField','controls/Table','controls/VirtualList','minecraft/UiScreen','minecraft/UiContainerScreen']
with zipfile.ZipFile(a.jar) as jar:
    names=jar.namelist();classes=[name for name in names if name.endswith('.class')]
    assert len(classes)>140, f'incomplete framework: {len(classes)} classes'
    for relative in required: assert f'com/nstut/openui/{relative}.class' in names, relative
    assert not any(name.startswith('net/minecraft/') for name in names),'game classes must never be bundled'
    for name in classes:
        actual=struct.unpack('>H',jar.read(name)[6:8])[0]
        assert actual<=major,(name,actual,major)
    if loader=='forge' and version in ('1.12.2','1.16.5'):
        pack=json.loads(jar.read('pack.mcmeta'))['pack']
        assert pack['pack_format']=={'1.12.2':3,'1.16.5':6}[version], 'incorrect native resource pack format'
        assert isinstance(pack.get('description'),str) and pack['description'], 'missing resource pack description'
    if loader=='fabric':
        meta=json.loads(jar.read('fabric.mod.json'))
        assert meta['id']=='openui_mc' and meta['depends']['minecraft']==version
        assert meta['environment']=='*','server dependency container must remain discoverable'
    elif version=='1.12.2':
        meta=json.loads(jar.read('mcmod.info'))[0]
        assert meta['modid']=='openui_mc' and meta['mcversion']==version
        assert not meta.get('clientSideOnly',False)
    else:
        path='META-INF/neoforge.mods.toml' if loader=='neoforge' else 'META-INF/mods.toml'
        metadata=jar.read(path).decode()
        assert 'openui_mc' in metadata
        if loader=='neoforge' and version=='26.2':
            assert 'iconFile="openui_mc.png"' in metadata and 'logoFile=' not in metadata, 'deprecated NeoForge icon metadata'
            assert 'openui_mc.png' in names, 'missing declared native icon'
    print(f'{a.target}: {len(classes)} classes, bytecode <= {major}, complete API and metadata verified')
