package com.nstut.openui.layout;

public record Size(int width, int height) {
    public Size {
        width = Math.max(0, width);
        height = Math.max(0, height);
    }
}

