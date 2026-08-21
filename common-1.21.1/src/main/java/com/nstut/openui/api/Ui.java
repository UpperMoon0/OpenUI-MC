package com.nstut.openui.api;

import com.nstut.openui.controls.SignalText;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.controls.Checkbox;
import com.nstut.openui.controls.ProgressBar;
import com.nstut.openui.controls.Slider;
import com.nstut.openui.controls.SwitchControl;
import com.nstut.openui.controls.VirtualList;
import com.nstut.openui.controls.DynamicGrid;
import com.nstut.openui.controls.SignalSwitcher;
import com.nstut.openui.controls.AsyncComponent;
import com.nstut.openui.state.AsyncValue;
import com.nstut.openui.layout.Insets;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Signal;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.function.Function;
import java.util.List;
import java.util.function.Supplier;

public final class Ui {
    private Ui() { }

    public static HStack row(UIComponent... children) {
        HStack row = new HStack();
        for (UIComponent child : children) row.addChild(child);
        return row;
    }

    public static VStack column(UIComponent... children) {
        VStack column = new VStack();
        for (UIComponent child : children) column.addChild(child);
        return column;
    }

    public static Stack stack(UIComponent... children) {
        Stack stack = new Stack();
        for (UIComponent child : children) stack.addChild(child);
        return stack;
    }

    public static ClipStack clip(UIComponent... children) {
        ClipStack stack = new ClipStack();
        for (UIComponent child : children) stack.addChild(child);
        return stack;
    }

    public static Positioned positioned(UIComponent child) { return new Positioned(child); }
    public static Padding padding(int all, UIComponent child) { return new Padding(all, child); }
    public static Padding padding(int vertical, int horizontal, UIComponent child) { return new Padding(vertical, horizontal, child); }
    public static Padding padding(Insets insets, UIComponent child) { return new Padding(insets, child); }
    public static Responsive responsive(Function<Responsive.Context, UIComponent> builder) { return new Responsive(builder); }
    public static Spacer spacer() { return new Spacer(); }
    public static Divider divider() { return new Divider(UiTheme.BORDER_SUBTLE); }
    public static TextWidget text(String text) { return TextWidget.label(text, UiTheme.TEXT_PRIMARY); }
    public static TextWidget text(Component text) { return text(text.getString()); }
    public static TextWidget heading(String text) { return TextWidget.label(text, UiTheme.TEXT_PRIMARY); }
    public static SignalText text(ReadableSignal<String> text) { return new SignalText(text, UiTheme.TEXT_PRIMARY, false); }

    public static ButtonWidget button(String label, Runnable action) {
        return new ButtonWidget(label, UiTheme.SURFACE_RAISED, UiTheme.SURFACE_HOVER, UiTheme.TEXT_PRIMARY).onPress(action).secondary();
    }

    public static ButtonWidget button(Component label, Runnable action) { return button(label.getString(), action); }
    public static TextField textField(Signal<String> value) { return new TextField(value); }
    public static TextField textField(Signal<String> value, Font font) { return new TextField(value, font); }
    public static Checkbox checkbox(String label, Signal<Boolean> value) { return new Checkbox(label, value); }
    public static SwitchControl toggle(Signal<Boolean> value) { return new SwitchControl(value); }
    public static ProgressBar progress(ReadableSignal<? extends Number> value) { return new ProgressBar(value); }
    public static Slider slider(Signal<Double> value, double min, double max) { return new Slider(value, min, max); }
    public static <T> VirtualList<T> list(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) { return new VirtualList<>(items, renderer); }
    public static <T> DynamicGrid<T> grid(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) { return new DynamicGrid<>(items, renderer); }
    public static <T> SignalSwitcher<T> switcher(ReadableSignal<T> selection) { return new SignalSwitcher<>(selection); }
    public static <T> AsyncComponent<T> async(ReadableSignal<AsyncValue<T>> state, Supplier<UIComponent> loading,
                                               Function<T, UIComponent> success, Function<Throwable, UIComponent> error) {
        return new AsyncComponent<>(state, loading, success, error);
    }
}
