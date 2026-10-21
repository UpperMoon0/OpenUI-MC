#!/usr/bin/env python3
"""Run the same legacy Java 8 compatibility regressions with Fabric's official names."""
from pathlib import Path
import argparse
from fabric_1165_sources import convert,ROOT
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);output=p.parse_args().output
source=ROOT/'forge-1.16.5/src/test/java';generated=set()
for path in source.rglob('*.java'):
    target=output/path.relative_to(source);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(convert(path.read_text()),encoding='utf-8');generated.add(target)
for stale in output.rglob('*.java'):
    if stale not in generated:stale.unlink()
