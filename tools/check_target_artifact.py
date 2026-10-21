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
        assert 'openui_mc' in jar.read(path).decode()
    print(f'{a.target}: {len(classes)} classes, bytecode <= {major}, complete API and metadata verified')
