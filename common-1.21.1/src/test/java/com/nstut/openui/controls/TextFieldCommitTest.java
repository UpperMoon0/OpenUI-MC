package com.nstut.openui.controls;

import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TextFieldCommitTest {
    private static Font font() { return new Font(null, false) {
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
            public void add(AbstractWidget widget) {}
            public void remove(AbstractWidget widget) {}
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

    @Test void normalizationCanClearFocusDuringCommit() { assertCallbackFocusChange(false); }
    @Test void normalizationCanCloseRuntimeDuringCommit() { assertCallbackFocusChange(true); }

    private void assertCallbackFocusChange(boolean close) {
        Signal<String> value=Signals.of("1");
        TextField field=new TextField(value,font());
        AtomicInteger commits=new AtomicInteger();
        UiRuntime runtime=new UiRuntime(font(),new NativeWidgetHost() {
            public void add(AbstractWidget widget) {}
            public void remove(AbstractWidget widget) {}
        });
        field.onCommit(text -> {
            commits.incrementAndGet();
            value.set(Integer.toString(Integer.parseInt(text)));
            if (close) runtime.close(); else runtime.focus().clearFocus();
        });
        try {
            runtime.setRoot(field); runtime.setViewport(0,0,100,24); field.requestFocus();
            field.setValue("02"); assertTrue(runtime.keyPressed(257,0,0));
            assertEquals("2",field.getValue()); assertEquals(1,commits.get());
            field.onFocusLost(); assertEquals(1,commits.get());
            assertTrue(field.keyPressed(335,0,0)); assertEquals(2,commits.get());
        } finally { runtime.close(); }
    }

    @Test void failedCallbackReleasesGuardAndDoesNotMarkValueSubmitted() {
        TextField field=new TextField(Signals.of("first"),font());
        AtomicInteger commits=new AtomicInteger();
        field.onCommit(text -> {
            int attempt=commits.incrementAndGet();
            field.setValue(text.trim());
            field.onFocusLost();
            if (attempt==1) throw new IllegalStateException("commit failed");
        });
        field.setValue(" second ");
        assertThrows(IllegalStateException.class,()->field.keyPressed(257,0,0));
        assertEquals("second",field.getValue()); assertEquals(1,commits.get());
        field.onFocusLost(); assertEquals(2,commits.get());
        field.onFocusLost(); assertEquals(2,commits.get());
    }
}
