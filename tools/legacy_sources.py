#!/usr/bin/env python3
"""Generate the Java-8/source adapters for the independent Forge 1.16.5 target.

Modern ports remain untouched. Record lowering preserves constructors, accessors,
equality and hash semantics. Jabel handles modern syntax; --release 8 checks APIs.
"""
import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def end_pair(text, start, opening='(', closing=')'):
    depth=0; quote=None; escaped=False; comment=None; i=start
    while i<len(text):
        ch=text[i]; nxt=text[i:i+2]
        if comment:
            if comment=='line' and ch=='\n': comment=None
            elif comment=='block' and nxt=='*/': comment=None; i+=1
        elif quote:
            if escaped: escaped=False
            elif ch=='\\': escaped=True
            elif ch==quote: quote=None
        elif nxt=='//': comment='line'; i+=1
        elif nxt=='/*': comment='block'; i+=1
        elif ch in '\"\'': quote=ch
        elif ch==opening: depth+=1
        elif ch==closing:
            depth-=1
            if not depth:return i
        i+=1
    raise ValueError('Unbalanced source near '+text[start:start+80])

def split_params(params):
    result=[]; start=0; depth=0
    for i,ch in enumerate(params):
        if ch in '<([':depth+=1
        elif ch in '>)]':depth-=1
        elif ch==',' and depth==0: result.append(params[start:i].strip());start=i+1
    if params[start:].strip(): result.append(params[start:].strip())
    return result

def surface(text):
    out=[];i=0
    while i<len(text):
        if text[i]=='{':
            i=end_pair(text,i,'{','}')+1;out.append('{}')
        else:out.append(text[i]);i+=1
    return ''.join(out)

def lower_records(text, nested_context=False):
    while True:
        match=re.search(r'(?P<access>public|protected|private)?\s*(?P<static>static\s+)?\brecord\s+(?P<name>\w+)(?P<generic><[^{}()]+>)?\s*\(',text)
        if not match:return text
        start=match.end()-1; end=end_pair(text,start); body_start=text.index('{',end); body_end=end_pair(text,body_start,'{','}')
        name=match['name']; generic=match['generic'] or ''; params=split_params(text[start+1:end]); fields=[]
        for param in params:
            typ,field=param.rsplit(None,1);fields.append((typ,field))
        signature=', '.join(params); assignments=' '.join('this.%s=%s;'%(f,f) for t,f in fields)
        declared=surface(text[body_start+1:body_end]); body=lower_records(text[body_start+1:body_end], True); compact=re.search(r'\b(public|private|protected)?\s*'+name+r'\s*\{',body)
        has_canonical=False
        if compact:
            cb=body.index('{',compact.start());ce=end_pair(body,cb,'{','}')
            body=body[:compact.start()]+'public '+name+'('+signature+') {'+body[cb+1:ce]+assignments+'}'+body[ce+1:]
            has_canonical=True
        else:
            for ctor in re.finditer(r'\b'+name+r'\s*\(',body):
                ce=end_pair(body,ctor.end()-1)
                existing=split_params(body[ctor.end():ce])
                if existing==params:has_canonical=True
        nested=nested_context or text[:match.start()].count('{')>text[:match.start()].count('}')
        prefix=(match['access']+' ' if match['access'] else '')+('static ' if nested else '')+'final class '+name+generic+text[end+1:body_start]+' {'
        generated='\n'+''.join('private final %s %s;\n'%(t,f) for t,f in fields)
        if not has_canonical:generated+='public '+name+'('+signature+') {'+assignments+'}\n'
        for typ,field in fields:
            if not re.search(r'public\s+[^;{}\n]+\b'+field+r'\s*\(\s*\)',declared):generated+='public '+typ+' '+field+'() {return '+field+';}\n'
        if not re.search(r'boolean\s+equals\(',declared):
            generated+='@Override public boolean equals(Object __other) {if(this==__other)return true;if(!(__other instanceof '+name+'))return false;'+name+' __v=('+name+')__other;return '+(' && '.join('java.util.Objects.equals(this.%s,__v.%s)'%(f,f) for t,f in fields) or 'true')+';}\n'
        if not re.search(r'int\s+hashCode\(',declared):generated+='@Override public int hashCode(){int __h=0;'+''.join('__h=31*__h+java.util.Objects.hashCode(%s);'%f for t,f in fields)+'return __h;}\n'
        if not re.search(r'String\s+toString\(',declared):generated+='@Override public String toString(){return "'+name+'["+'+(' + ", "+'.join('"'+f+'="+String.valueOf('+f+')' for t,f in fields) or '""')+'+"]";}\n'
        text=text[:match.start()]+'\n'+prefix+generated+body+'}'+text[body_end+1:]

def convert(text):
    if 'public class ClipStack extends Stack {' in text:
        # LegacyGuiGraphics transforms each local rectangle before intersecting
        # its native stack. Modern ClipStack's pre-intersection mixes local
        # coordinate spaces when parent and child have different poses.
        text, count = re.subn(r'    private static final Deque<ScissorRect> STACK.*?(?=    @Override)',
            """    public static void push(GuiGraphics graphics, int x, int y, int width, int height) {
        if (graphics != null) graphics.enableScissor(x, y, x + width, y + height);
    }

    public static void pop(GuiGraphics graphics) {
        if (graphics != null) graphics.disableScissor();
    }

""", text, count=1, flags=re.S)
        if count != 1:
            raise ValueError('ClipStack source changed; review the legacy clipping adapter')
        text = text.replace('import java.util.ArrayDeque;\n', '').replace('import java.util.Deque;\n', '')
    text=lower_records(text)
    text=re.sub(r"\b(?:non-)?sealed\s+", "", text)
    text=re.sub(r"\bpermits\s+[\w.,\s]+(?=\{)", "", text)
    replacements={
      'net.minecraft.client.gui.GuiGraphics':'com.nstut.openui.graphics.LegacyGuiGraphics',
      'net.minecraft.client.gui.Font':'net.minecraft.client.gui.FontRenderer',
      'net.minecraft.client.gui.components.AbstractWidget':'net.minecraft.client.gui.widget.Widget',
      'net.minecraft.client.gui.components.EditBox':'net.minecraft.client.gui.widget.TextFieldWidget',
      'net.minecraft.client.gui.screens.inventory.AbstractContainerScreen':'net.minecraft.client.gui.screen.inventory.ContainerScreen',
      'net.minecraft.client.gui.screens.Screen':'net.minecraft.client.gui.screen.Screen',
      'net.minecraft.client.resources.sounds.SimpleSoundInstance':'net.minecraft.client.audio.SimpleSound',
      'net.minecraft.network.chat.Component':'net.minecraft.util.text.ITextComponent',
      'net.minecraft.network.chat.FormattedText':'net.minecraft.util.text.ITextProperties',
      'net.minecraft.network.chat.Style':'net.minecraft.util.text.Style',
      'net.minecraft.resources.ResourceLocation':'net.minecraft.util.ResourceLocation',
      'net.minecraft.sounds.SoundEvents':'net.minecraft.util.SoundEvents',
      'net.minecraft.util.FormattedCharSequence':'net.minecraft.util.IReorderingProcessor',
      'net.minecraft.world.item.ItemStack':'net.minecraft.item.ItemStack',
      'net.minecraft.world.entity.player.Inventory':'net.minecraft.entity.player.PlayerInventory',
      'net.minecraft.world.inventory.AbstractContainerMenu':'net.minecraft.inventory.container.Container',
      'net.minecraft.Util':'net.minecraft.util.Util',
    }
    for old,new in replacements.items():text=text.replace(old,new)
    names={'GuiGraphics':'LegacyGuiGraphics','Font':'FontRenderer','AbstractWidget':'Widget','EditBox':'TextFieldWidget','SimpleSoundInstance':'SimpleSound','Component':'ITextComponent','FormattedText':'ITextProperties','FormattedCharSequence':'IReorderingProcessor','AbstractContainerScreen':'ContainerScreen','AbstractContainerMenu':'Container','Inventory':'PlayerInventory'}
    for old,new in names.items():text=re.sub(r'\b'+old+r'\b',new,text)
    text=text.replace('ITextComponent.empty()','new net.minecraft.util.text.StringTextComponent("")').replace('ITextComponent.literal(','new net.minecraft.util.text.StringTextComponent(').replace('ITextComponent.translatable(','new net.minecraft.util.text.TranslationTextComponent(')
    for coll,method in [('List','of'),('List','copyOf'),('Set','of'),('Set','copyOf'),('Map','of'),('Map','copyOf')]:
        text=text.replace(coll+'.'+method+'(', 'com.nstut.openui.compat.Java8.'+coll.lower()+('Copy' if method=='copyOf' else '')+'(')
    text=text.replace('.toList()', '.collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList))')
    text=text.replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(')
    text=text.replace('new FontRenderer(null, false)','new FontRenderer(null)')
    text=text.replace('editBox.setFocused(', 'editBox.setFocus(')
    text=text.replace('java.util.com.nstut','com.nstut').replace('List::of','java.util.Collections::emptyList')
    text=text.replace('RenderSystem.setShaderColor(', 'RenderSystem.color4f(')
    text=text.replace('Objects.requireNonNullElse(', 'com.nstut.openui.compat.Java8.requireNonNullElse(')
    text=re.sub(r'([A-Za-z_]\w*(?:\.[A-Za-z_]\w*(?:\([^()]*\))?)*)\.isBlank\(\)',r'com.nstut.openui.compat.Java8.isBlank(\1)',text)
    text=re.sub(r'("(?:\\.|[^"\\])*")\.repeat\(([^()]*)\)',r'com.nstut.openui.compat.Java8.repeat(\1,\2)',text)
    text=text.replace('editBox.setY(y + (height - font.lineHeight) / 2);','editBox.y=y + (height - font.lineHeight) / 2;').replace('editBox.isActive()','editBox.active')
    return text

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True,type=Path);args=parser.parse_args()
    generated = set()
    for source in [ROOT/'shared/core/src/main/java',ROOT/'common-1.20.1/src/main/java']:
        for path in source.rglob('*.java'):
            relative=path.relative_to(source)
            if relative.name in ['UiScreen.java','UiContainerScreen.java']:continue
            target=args.output/relative;generated.add(target);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(convert(path.read_text()),encoding='utf-8')
    # Synchronize the dedicated generated source tree after successful conversion.
    # Incremental builds must not compile classes deleted or renamed upstream.
    for obsolete in args.output.rglob('*.java'):
        if obsolete not in generated:
            obsolete.unlink()
if __name__=='__main__':main()
