package com.nstut.openui.mixin;

import com.nstut.openui.api.ShadowlessEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
abstract class EditBoxMixin {
    private boolean openui$shadow() {
        return !((Object) this instanceof ShadowlessEditBox);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int openui$drawFormatted(GuiGraphics graphics, Font font, FormattedCharSequence text,
                                     int x, int y, int color) {
        return graphics.drawString(font, text, x, y, color, openui$shadow());
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"))
    private int openui$drawComponent(GuiGraphics graphics, Font font, Component text,
                                     int x, int y, int color) {
        return graphics.drawString(font, text, x, y, color, openui$shadow());
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
    private int openui$drawString(GuiGraphics graphics, Font font, String text,
                                  int x, int y, int color) {
        return graphics.drawString(font, text, x, y, color, openui$shadow());
    }
}
