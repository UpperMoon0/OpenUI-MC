package com.nstut.openui.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import java.util.ArrayList;
import java.util.List;
/** Preserves native 1.12 font metrics, formatting, bidi and wrapping. */
public final class LegacyFont {
    private final FontRenderer nativeFont;
    public final int lineHeight;
    public LegacyFont(FontRenderer font) { nativeFont=font;lineHeight=font==null?9:font.FONT_HEIGHT; }
    public static LegacyFont current() { return new LegacyFont(Minecraft.getMinecraft()==null?null:Minecraft.getMinecraft().fontRenderer); }
    public FontRenderer nativeFont() { return nativeFont; }
    public int width(String text) { return nativeFont==null?text.length()*6:nativeFont.getStringWidth(text); }
    public int width(ITextComponent text) { return width(text.getFormattedText()); }
    public int width(LegacyOrderedText text) { return width(text.text()); }
    public String plainSubstrByWidth(String text,int width) { return plainSubstrByWidth(text,width,false); }
    public String plainSubstrByWidth(String text,int width,boolean reverse) {
        if(nativeFont!=null)return nativeFont.trimStringToWidth(text,Math.max(0,width),reverse);
        int count=Math.max(0,Math.min(text.length(),width/6));return reverse?text.substring(text.length()-count):text.substring(0,count);
    }
    public List<LegacyOrderedText> split(ITextComponent text,int width) {
        List<LegacyOrderedText> result=new ArrayList<>();
        if(nativeFont==null) { result.add(LegacyOrderedText.from(text));return result; }
        for(String line:nativeFont.listFormattedStringToWidth(text.getFormattedText(),Math.max(1,width)))result.add(new LegacyOrderedText(line));
        return result;
    }
}
