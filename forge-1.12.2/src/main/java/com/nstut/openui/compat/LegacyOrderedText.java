package com.nstut.openui.compat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
/** A native 1.12 formatted line. Formatting codes stay intact for FontRenderer. */
public final class LegacyOrderedText {
    private final String text;
    public LegacyOrderedText(String text) { this.text=text==null?"":text; }
    public static LegacyOrderedText from(ITextComponent component) { return new LegacyOrderedText(component.getFormattedText()); }
    public static LegacyOrderedText forward(String text,Style style) { return new LegacyOrderedText(style.getFormattingCode()+text); }
    public static LegacyOrderedText composite(LegacyOrderedText a,LegacyOrderedText b) { return new LegacyOrderedText(a.text+b.text); }
    public String text() { return text; }
    public interface Visitor { boolean accept(int index,Style style,int codePoint); }
    public boolean accept(Visitor visitor) {
        // FontRenderer formatting markers are not visible glyphs.
        String plain=net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(text);
        for(int i=0;i<plain.length();) { int cp=plain.codePointAt(i); if(!visitor.accept(i,new Style(),cp))return false;i+=Character.charCount(cp); }
        return true;
    }
}
