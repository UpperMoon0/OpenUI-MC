package com.nstut.openui.input;

import com.nstut.openui.api.Ui;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventTest {
    @Test
    void listenersCanStopPropagationAndPreventDefault() {
        var button = Ui.button("Buy", () -> { });
        List<String> calls = new ArrayList<>();
        button.on(EventType.CLICK, event -> {
            calls.add(event.phase().name());
            event.preventDefault();
            event.stopPropagation();
        });
        PointerEvent event = new PointerEvent(EventType.CLICK, button, 1, 1, 0, 0, 0);
        event.route(button, EventPhase.TARGET);
        button.dispatchEvent(event, EventPhase.TARGET);
        assertEquals(List.of("TARGET"), calls);
    }
}
