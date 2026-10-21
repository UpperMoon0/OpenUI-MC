#!/usr/bin/env python3
"""Verify direct, single-owner modern UI source wiring (no generated clones)."""
import argparse
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MODULES=('neoforge-26.1.2','fabric-26.2','neoforge-26.2')
SHARED_SCRIPT="apply from: file('../gradle/modern-ui-sources.gradle')"

def inventory(root, module, kind='main'):
    if module not in MODULES:raise ValueError('Not a modern UI module: '+module)
    build=(root/module/'build.gradle').read_text()
    if build.count(SHARED_SCRIPT)!=1:raise ValueError('Modern module must apply authoritative source wiring: '+module)
    script=(root/'gradle/modern-ui-sources.gradle').read_text()
    required=("file('../shared/core/src/main/java')","file('../shared/modern/src/main/java')","file('../shared/modern/src/test/java')")
    if not all(path in script for path in required):raise ValueError('Authoritative modern source roots are missing')
    if any((root/'common-26.2/src').rglob('*.java')):raise ValueError('A hand-maintained 26.2 Java clone has reappeared')
    roots=[root/'shared/modern/src'/kind/'java',root/module/'src'/kind/'java']
    if kind=='main':roots.insert(0,root/'shared/core/src/main/java')
    result={}
    for source in roots:
        for path in sorted(source.rglob('*.java')):
            relative=path.relative_to(source)
            if relative in result:raise ValueError('Native module shadows shared class: '+str(relative))
            if source==root/module/'src/main/java' and not any(part in relative.parts for part in ('fabric','neoforge')):
                raise ValueError('Loader-neutral modern Java must live in shared/modern: '+str(path))
            result[relative]=path
    return result

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--module',choices=MODULES,required=True);args=parser.parse_args()
    main=inventory(ROOT,args.module);tests=inventory(ROOT,args.module,'test')
    print(f'{args.module}: {len(main)} production and {len(tests)} test sources use authoritative modern roots')
