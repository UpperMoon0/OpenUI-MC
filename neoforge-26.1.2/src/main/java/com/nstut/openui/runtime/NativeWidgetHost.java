package com.nstut.openui.runtime;

import net.minecraft.client.gui.components.AbstractWidget;

public interface NativeWidgetHost {
    void add(AbstractWidget widget);
    void remove(AbstractWidget widget);
}

