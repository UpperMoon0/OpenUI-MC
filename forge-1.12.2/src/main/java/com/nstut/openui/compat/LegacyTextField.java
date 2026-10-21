package com.nstut.openui.compat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.util.text.ITextComponent;
import java.util.function.Consumer;
/** Bridges native 1.12 text editing while OpenUI retains clipping, focus and painting. */
public final class LegacyTextField extends LegacyWidget {
    private GuiTextField field;
    private final LegacyFont font;
    private int nativeWidth,nativeHeight,maxLength=32,textColor=14737632,disabledColor=7368816;
    private boolean bordered=true;
    private Consumer<String> responder=ignored->{};
    public LegacyTextField(LegacyFont font,int x,int y,int width,int height,ITextComponent title) {
        this.font=font;this.x=x;this.y=y;this.width=width;this.height=height;nativeWidth=width;nativeHeight=height;
        field=new GuiTextField(0,font.nativeFont(),x,y,width,height);
        field.setGuiResponder(new GuiPageButtonList.GuiResponder() {
            public void setEntryValue(int id,boolean value) { }
            public void setEntryValue(int id,float value) { }
            public void setEntryValue(int id,String value) { responder.accept(value); }
        });
    }
    private void sync() {
        if(width!=nativeWidth || height!=nativeHeight) {
            String value=field.getText();int cursor=field.getCursorPosition(),selection=field.getSelectionEnd();boolean focused=field.isFocused();
            field=new GuiTextField(0,font.nativeFont(),x,y,width,height);nativeWidth=width;nativeHeight=height;
            field.setMaxStringLength(maxLength);field.setEnableBackgroundDrawing(bordered);field.setTextColor(textColor);field.setDisabledTextColour(disabledColor);
            field.setText(value);field.setCursorPosition(cursor);field.setSelectionPos(selection);field.setFocused(focused);
            field.setGuiResponder(new GuiPageButtonList.GuiResponder() {
                public void setEntryValue(int id,boolean value) { }
                public void setEntryValue(int id,float value) { }
                public void setEntryValue(int id,String value) { responder.accept(value); }
            });
        }
        field.x=x;field.y=y;field.setVisible(visible);field.setEnabled(active);
    }
    public void tick() { field.updateCursorCounter(); }
    public void setResponder(Consumer<String> listener) { responder=listener; }
    public String getValue() { return field.getText(); }
    public void setValue(String value) { String before=field.getText();field.setText(value);if(!before.equals(field.getText()))responder.accept(field.getText()); }
    public void setCursorPosition(int value) { field.setCursorPosition(value); }
    public int getCursorPosition() { return field.getCursorPosition(); }
    public void setHighlightPos(int value) { field.setSelectionPos(value); }
    public void setMaxLength(int value) { maxLength=value;field.setMaxStringLength(value); }
    public void setBordered(boolean value) { bordered=value;field.setEnableBackgroundDrawing(value); }
    public void setTextColor(int color) { textColor=color;field.setTextColor(color); }
    public void setTextColorUneditable(int color) { disabledColor=color;field.setDisabledTextColour(color); }
    public void setFocus(boolean value) { field.setFocused(value); }
    public boolean isFocused() { return field.isFocused(); }
    public void setX(int value) { x=value; }
    public void setWidth(int value) { width=value; }
    public boolean keyPressed(int key,int scanCode,int modifiers) { sync();return active&&visible&&field.textboxKeyTyped('\0',scanCode); }
    public boolean charTyped(char value,int modifiers) { sync();return active&&visible&&field.textboxKeyTyped(value,0); }
    public boolean mouseClicked(double mx,double my,int button) { sync();return active&&visible&&field.mouseClicked((int)mx,(int)my,button); }
}
