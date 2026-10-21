package com.nstut.openui.minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
public final class UiPlatform {
    private UiPlatform() { }
    public static void playClickSound() { try { Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK,1.0F)); } catch(Throwable ignored) { } }
    public static void setClipboard(String text) { if(text!=null)try { GuiScreen.setClipboardString(text); } catch(Throwable ignored) { } }
    public static String getClipboard() { try { return GuiScreen.getClipboardString(); } catch(Throwable ignored) { return ""; } }
}
