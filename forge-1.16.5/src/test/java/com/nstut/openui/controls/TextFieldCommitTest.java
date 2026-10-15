package com.nstut.openui.controls;

import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TextFieldCommitTest {
    private static FontRenderer font() { return new FontRenderer(null) {
        @Override public String plainSubstrByWidth(String value, int width) { return value.substring(0,Math.min(value.length(),Math.max(0,width/6))); }
        @Override public int width(String value) { return value.length()*6; }
    }; }

    @Test void normalizedSignalValueIsAlreadySubmittedOnBlur() {
        Signal<String> value=Signals.of("1");
        TextField field=new TextField(value,font());
        AtomicInteger commits=new AtomicInteger();
        field.onCommit(text -> {
            commits.incrementAndGet();
            value.set(Integer.toString(Integer.parseInt(text)));
        });
        UiRuntime runtime=new UiRuntime(font(),new NativeWidgetHost() {
            public void add(Widget widget) {}
            public void remove(Widget widget) {}
        });
        try {
            runtime.setRoot(field); runtime.setViewport(0,0,100,24); field.requestFocus();
            field.setValue("02"); assertTrue(runtime.keyPressed(257,0,0));
            assertEquals("2",field.getValue()); assertEquals(1,commits.get());
            runtime.focus().clearFocus(); assertEquals(1,commits.get());
            field.requestFocus(); runtime.focus().clearFocus(); assertEquals(1,commits.get());
            field.requestFocus(); field.setValue("03"); runtime.focus().clearFocus();
            assertEquals("3",field.getValue()); assertEquals(2,commits.get());
            field.requestFocus(); runtime.focus().clearFocus(); assertEquals(2,commits.get());
            field.requestFocus(); assertTrue(runtime.keyPressed(335,0,0));
            assertEquals(3,commits.get());
            runtime.focus().clearFocus(); assertEquals(3,commits.get());
        } finally { runtime.close(); }
    }

    @Test void directCallbackNormalizationDoesNotRepeatOnUnchangedBlur() {
        TextField field=new TextField(Signals.of("first"),font());
        AtomicInteger commits=new AtomicInteger();
        field.onCommit(text -> { commits.incrementAndGet(); field.setValue(text.trim()); });
        field.setValue(" second "); field.onFocusLost();
        assertEquals("second",field.getValue()); assertEquals(1,commits.get());
        field.onFocusLost(); assertEquals(1,commits.get());
        assertTrue(field.keyPressed(257,0,0)); assertEquals(2,commits.get());
        field.onFocusLost(); assertEquals(2,commits.get());
    }
}
