package com.nstut.openui.api;

import com.nstut.openui.controls.SignalText;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTextDefaultsTest {

    @Test
    void bodyTextWrapsWithoutEllipsisByDefault() throws Exception {
        assertBodyDefaults(Ui.text("A normal body sentence"));
        assertBodyDefaults(Ui.text(net.minecraft.network.chat.Component.literal("Component body text")));

        Signal<String> signal = Signals.of("Reactive body text");
        SignalText reactive = Ui.text(signal);
        assertBodyDefaults(reactive);
    }

    @Test
    void compactTextCanExplicitlyOptIntoSingleLineEllipsis() throws Exception {
        TextWidget compact = Ui.text("Compact label").nowrap().ellipsis();

        assertFalse(flag(compact, "wrap"));
        assertTrue(flag(compact, "ellipsis"));
    }

    private static void assertBodyDefaults(TextWidget widget) throws Exception {
        assertTrue(flag(widget, "wrap"));
        assertFalse(flag(widget, "ellipsis"));
    }

    private static boolean flag(TextWidget widget, String fieldName) throws Exception {
        Field field = TextWidget.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(widget);
    }
}
