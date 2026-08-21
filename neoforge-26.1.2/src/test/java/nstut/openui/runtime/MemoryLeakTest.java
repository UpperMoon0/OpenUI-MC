package com.nstut.openui.runtime;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.component.LifecycleState;
import com.nstut.openui.controls.Checkbox;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemoryLeakTest {

    @Test
    void runtimeCloseDisposesComponentsAndOverlaysCleanly() {
        List<AbstractWidget> activeWidgets = new ArrayList<>();
        NativeWidgetHost host = new NativeWidgetHost() {
            @Override public void add(AbstractWidget widget) { activeWidgets.add(widget); }
            @Override public void remove(AbstractWidget widget) { activeWidgets.remove(widget); }
        };

        UiRuntime runtime = new UiRuntime(new Font(null), host);

        Signal<Boolean> checked = Signals.of(true);

        ButtonWidget btn = Ui.button("test", () -> {});
        Checkbox cb = Ui.checkbox("Check", checked);

        UIComponent root = Ui.column(btn, cb);
        runtime.setRoot(root);

        assertEquals(LifecycleState.MOUNTED, root.lifecycleState());
        assertEquals(LifecycleState.MOUNTED, btn.lifecycleState());
        assertEquals(LifecycleState.MOUNTED, cb.lifecycleState());

        // Show an overlay
        UIComponent overlay = Ui.card(Ui.text("Overlay"));
        runtime.overlays().show(OverlayLayer.MODAL, overlay);
        assertEquals(1, runtime.overlays().size());

        // Close runtime
        runtime.close();

        assertEquals(LifecycleState.UNMOUNTED, root.lifecycleState());
        assertEquals(0, runtime.overlays().size());
        assertEquals(0, runtime.animations().activeCount());
        assertNull(runtime.focus().focused());
        assertNull(runtime.root());
    }
}
