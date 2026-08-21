package com.nstut.openui.controls;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.Stack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.layout.Alignment;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.overlay.OverlayManager;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.network.chat.Component;

import java.util.Objects;

public final class Dialog {
    private Dialog() { }

    public static OverlayHandle show(OverlayManager overlays, UIComponent content) {
        return show(overlays, content, true, true, null);
    }

    public static OverlayHandle show(OverlayManager overlays, UIComponent content,
                                    boolean closeOnEscape, boolean closeOnBackdropClick, Runnable onClose) {
        if (overlays == null || content == null) return null;
        Stack modalStack = new Stack().align(Alignment.CENTER, Alignment.CENTER).child(content);
        return overlays.show(
                OverlayLayer.MODAL,
                modalStack,
                true,
                closeOnEscape,
                closeOnBackdropClick,
                onClose
        );
    }

    public static OverlayHandle confirm(OverlayManager overlays, String title, String message, Runnable confirm, Runnable cancel) {
        return confirm(overlays, Component.literal(title), Component.literal(message), confirm, cancel);
    }

    public static OverlayHandle confirm(OverlayManager overlays, Component title, Component message, Runnable confirm, Runnable cancel) {
        Card card = new Card().elevated(true).outlined(true).padding(14);
        OverlayHandle[] handleRef = new OverlayHandle[1];

        ButtonWidget cancelBtn = Ui.button(Component.translatable("gui.cancel"), () -> {
            if (handleRef[0] != null) handleRef[0].close();
            if (cancel != null) cancel.run();
        }).ghost();

        ButtonWidget confirmBtn = Ui.button(Component.translatable("gui.ok"), () -> {
            if (handleRef[0] != null) handleRef[0].close();
            if (confirm != null) confirm.run();
        }).primary();

        card.addChild(Ui.column(
                Ui.heading(title),
                Ui.text(message),
                Ui.row(cancelBtn, confirmBtn).gap(6)
        ).gap(10));
        card.width(220).minHeight(90);

        OverlayHandle handle = show(overlays, card, true, true, cancel);
        handleRef[0] = handle;
        return handle;
    }

    public static OverlayHandle alert(OverlayManager overlays, Component title, Component message, Runnable onOk) {
        Card card = new Card().elevated(true).outlined(true).padding(14);
        OverlayHandle[] handleRef = new OverlayHandle[1];

        ButtonWidget okBtn = Ui.button(Component.translatable("gui.ok"), () -> {
            if (handleRef[0] != null) handleRef[0].close();
            if (onOk != null) onOk.run();
        }).primary();

        card.addChild(Ui.column(
                Ui.heading(title),
                Ui.text(message),
                Ui.row(okBtn)
        ).gap(10));
        card.width(200).minHeight(80);

        OverlayHandle handle = show(overlays, card, true, true, onOk);
        handleRef[0] = handle;
        return handle;
    }

    public static UIComponent confirm(String title, String message, Runnable confirm, Runnable cancel) {
        return confirm(Component.literal(title), Component.literal(message), confirm, cancel);
    }

    public static UIComponent confirm(Component title, Component message, Runnable confirm, Runnable cancel) {
        Card card = new Card().elevated(true).outlined(true).padding(14);
        card.addChild(Ui.column(
                Ui.heading(title),
                Ui.text(message),
                Ui.row(
                        Ui.button(Component.translatable("gui.cancel"), cancel).ghost(),
                        Ui.button(Component.translatable("gui.ok"), confirm).primary()
                ).gap(6)
        ).gap(10));
        card.width(220).minHeight(90);
        return new Stack().align(Alignment.CENTER, Alignment.CENTER).child(card);
    }
}
