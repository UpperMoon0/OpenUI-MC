package com.nstut.openui.controls;

import com.nstut.openui.api.Panel;
import com.nstut.openui.api.Positioned;
import com.nstut.openui.api.Stack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.UiTheme;
import com.nstut.openui.layout.Alignment;

public final class Dialog {
    private Dialog() { }

    public static UIComponent confirm(String title, String message, Runnable confirm, Runnable cancel) {
        Panel card = new Panel(UiTheme.SURFACE_RAISED, UiTheme.BORDER_STRONG)
                .radius(UiTheme.RADIUS_LG).padding(12).elevated();
        card.addChild(Ui.column(
                Ui.heading(title),
                Ui.text(message),
                Ui.row(
                        Ui.button("Cancel", cancel).ghost(),
                        Ui.button("Confirm", confirm).primary()).gap(6)
        ).gap(10));
        card.width(220).minHeight(90);
        return new Stack().align(Alignment.CENTER, Alignment.CENTER).child(card);
    }
}
