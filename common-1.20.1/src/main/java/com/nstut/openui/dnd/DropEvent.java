package com.nstut.openui.dnd;

import com.nstut.openui.api.UIComponent;

public record DropEvent<T>(T data, UIComponent source, double mouseX, double mouseY) {
}
