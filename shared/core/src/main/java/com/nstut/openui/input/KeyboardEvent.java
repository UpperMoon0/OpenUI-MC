package com.nstut.openui.input;

import com.nstut.openui.api.UIComponent;

public final class KeyboardEvent extends UiEvent {
    private final int key, scanCode, modifiers;
    private final int codePoint;

    public KeyboardEvent(EventType type, UIComponent target, int key, int scanCode, int modifiers, char character) {
        this(type, target, key, scanCode, modifiers, (int) character);
    }

    public KeyboardEvent(EventType type, UIComponent target, int key, int scanCode, int modifiers, int codePoint) {
        super(type, target);
        this.key = key;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.codePoint = codePoint;
    }

    public int key() { return key; }
    public int scanCode() { return scanCode; }
    public int modifiers() { return modifiers; }
    /** Legacy BMP accessor; supplementary input is available through codePoint(). */
    public char character() { return Character.isBmpCodePoint(codePoint) ? (char) codePoint : '\0'; }
    public int codePoint() { return codePoint; }
}
