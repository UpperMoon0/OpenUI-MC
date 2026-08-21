package com.nstut.openui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class UiPlatform {
    private UiPlatform() {}

    public static void playClickSound() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        } catch (Throwable ignored) {
            // Headless / unit test safe
        }
    }

    public static void setClipboard(String text) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.keyboardHandler != null && text != null) {
                mc.keyboardHandler.setClipboard(text);
            }
        } catch (Throwable ignored) {
            // Headless safe
        }
    }

    public static String getClipboard() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.keyboardHandler != null) {
                return mc.keyboardHandler.getClipboard();
            }
        } catch (Throwable ignored) {
            // Headless safe
        }
        return "";
    }
}
