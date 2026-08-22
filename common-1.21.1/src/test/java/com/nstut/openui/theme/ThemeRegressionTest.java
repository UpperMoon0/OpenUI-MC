package com.nstut.openui.theme;

import com.nstut.openui.api.Divider;
import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.SignalText;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeRegressionTest {

    private static final com.nstut.openui.runtime.NativeWidgetHost DUMMY_HOST = new com.nstut.openui.runtime.NativeWidgetHost() {
        @Override public void add(net.minecraft.client.gui.components.AbstractWidget widget) {}
        @Override public void remove(net.minecraft.client.gui.components.AbstractWidget widget) {}
    };

    @Test
    void dividerDefaultUsesRuntimeTheme() {
        Divider divider = Ui.divider();
        UiRuntime runtime = new UiRuntime(new net.minecraft.client.gui.Font(null, false), DUMMY_HOST);
        runtime.theme(Theme.light());
        divider.mount(runtime);

        assertEquals(Theme.light().colors().borderSubtle(), divider.theme().colors().borderSubtle());

        runtime.theme(Theme.dark());
        assertEquals(Theme.dark().colors().borderSubtle(), divider.theme().colors().borderSubtle());
        divider.unmount();
    }

    @Test
    void signalTextDefaultUsesRuntimeTheme() {
        Signal<String> textSig = Signals.of("Hello");
        SignalText text = Ui.text(textSig);
        UiRuntime runtime = new UiRuntime(new net.minecraft.client.gui.Font(null, false), DUMMY_HOST);
        runtime.theme(Theme.light());
        text.mount(runtime);

        assertEquals(Theme.light().colors().onSurface(), text.theme().colors().onSurface());

        runtime.theme(Theme.dark());
        assertEquals(Theme.dark().colors().onSurface(), text.theme().colors().onSurface());
        text.unmount();
    }
}
