package com.nstut.openui.input;

import com.nstut.openui.api.UIComponent;

public final class KeyboardEvent extends UiEvent {
    private final int key, scanCode, modifiers;
    private final char character;

    public KeyboardEvent(EventType type, UIComponent target, int key, int scanCode, int modifiers, char character) {
        super(type, target);
        this.key = key;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.character = character;
    }

    public int key() { return key; }
    public int scanCode() { return scanCode; }
    public int modifiers() { return modifiers; }
    public char character() { return character; }
}
