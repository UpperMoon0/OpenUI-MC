package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** Identifies OpenUI-owned edit boxes whose text is rendered without vanilla's shadow. */
public final class ShadowlessEditBox extends EditBox {
    public ShadowlessEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }
}
