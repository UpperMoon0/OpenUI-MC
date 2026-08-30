package com.nstut.openui.controls;

import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ToastTest {

    private final NativeWidgetHost dummyHost = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) {}
        @Override public void remove(AbstractWidget widget) {}
    };

    /** Deterministic metrics: 6px per character, 9px line height, 30 chars per wrapped line. */
    private Font stubFont() {
        return new Font(null, false) {
            @Override public int width(String text) { return text.length() * 6; }
            @Override public int width(FormattedText text) { return text.getString().length() * 6; }
            @Override public int width(FormattedCharSequence text) { return 40; }
            @Override public List<FormattedCharSequence> split(FormattedText text, int maxWidth) {
                int charsPerLine = Math.max(1, maxWidth / 6);
                String value = text.getString();
                List<FormattedCharSequence> lines = new ArrayList<>();
                for (int i = 0; i < value.length(); i += charsPerLine) {
                    lines.add(Component.literal(value.substring(i, Math.min(value.length(), i + charsPerLine))).getVisualOrderText());
                }
                if (lines.isEmpty()) lines.add(Component.literal("").getVisualOrderText());
                return lines;
            }
        };
    }

    @Test
    void variableHeightToastsStackWithoutOverlap() {
        Font font = stubFont();
        UiRuntime runtime = new UiRuntime(font, dummyHost);
        Toast first = Toast.info("First", "short");
        Toast second = Toast.info("Second",
                "A deliberately very long toast message that wraps onto several lines and therefore has a much greater height than the short toasts around it.");
        Toast third = Toast.info("Third", "short again");
        Toast.show(runtime.overlays(), first);
        Toast.show(runtime.overlays(), second);
        Toast.show(runtime.overlays(), third);

        runtime.overlays().layout(font, 0, 0, 400, 800);

        assertTrue(second.getHeight() > first.getHeight(), "The long toast wraps and grows taller");
        assertTrue(second.getY() >= first.getY() + first.getHeight(),
                "Each toast must start below the previous toast's bottom edge");
        assertTrue(third.getY() >= second.getY() + second.getHeight(),
                "Each toast must start below the previous toast's bottom edge");
    }
}
