package com.nstut.openui.api;

import com.nstut.openui.controls.AsyncComponent;
import com.nstut.openui.controls.Badge;
import com.nstut.openui.controls.BarChart;
import com.nstut.openui.controls.Card;
import com.nstut.openui.controls.Checkbox;
import com.nstut.openui.controls.Chip;
import com.nstut.openui.controls.ContextMenu;
import com.nstut.openui.controls.Dialog;
import com.nstut.openui.controls.DynamicGrid;
import com.nstut.openui.controls.EmptyState;
import com.nstut.openui.controls.ErrorBoundary;
import com.nstut.openui.controls.IconWidget;
import com.nstut.openui.controls.LineChart;
import com.nstut.openui.controls.LoadingOverlay;
import com.nstut.openui.controls.Popover;
import com.nstut.openui.controls.ProgressBar;
import com.nstut.openui.controls.Radio;
import com.nstut.openui.controls.Select;
import com.nstut.openui.controls.SignalSwitcher;
import com.nstut.openui.controls.SignalText;
import com.nstut.openui.controls.Skeleton;
import com.nstut.openui.controls.ScrollView;
import com.nstut.openui.controls.Slider;
import com.nstut.openui.controls.Sparkline;
import com.nstut.openui.controls.Spinner;
import com.nstut.openui.controls.SwitchControl;
import com.nstut.openui.controls.Table;
import com.nstut.openui.controls.Tabs;
import com.nstut.openui.controls.TextField;
import com.nstut.openui.controls.Toast;
import com.nstut.openui.controls.Tooltip;
import com.nstut.openui.controls.VirtualGrid;
import com.nstut.openui.controls.VirtualList;
import com.nstut.openui.layout.Insets;
import com.nstut.openui.state.AsyncValue;
import com.nstut.openui.state.ReadableSignal;
import com.nstut.openui.state.Signal;
import com.nstut.openui.theme.TextStyle;
import com.nstut.openui.controls.ScrollView;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Ui {
    private Ui() { }

    public static HStack row(UIComponent... children) { HStack row = new HStack(); for (UIComponent child : children) row.addChild(child); return row; }
    public static VStack column(UIComponent... children) { VStack column = new VStack(); for (UIComponent child : children) column.addChild(child); return column; }
    public static Stack stack(UIComponent... children) { Stack stack = new Stack(); for (UIComponent child : children) stack.addChild(child); return stack; }
    public static ClipStack clip(UIComponent... children) { ClipStack stack = new ClipStack(); for (UIComponent child : children) stack.addChild(child); return stack; }
    public static Card card(UIComponent... children) { Card card = new Card(); if (children.length == 1) card.addChild(children[0]); else if (children.length > 1) card.addChild(column(children)); return card; }

    public static Positioned positioned(UIComponent child) { return new Positioned(child); }
    public static Padding padding(int all, UIComponent child) { return new Padding(all, child); }
    public static Padding padding(int vertical, int horizontal, UIComponent child) { return new Padding(vertical, horizontal, child); }
    public static Padding padding(Insets insets, UIComponent child) { return new Padding(insets, child); }
    public static Responsive responsive(Function<Responsive.Context, UIComponent> builder) { return new Responsive(builder); }
    public static Spacer spacer() { return new Spacer(); }
    public static Divider divider() { return new Divider(); }

    public static TextWidget text(String text) { return new TextWidget(text); }
    public static TextWidget text(Component text) { return new TextWidget(text); }
    public static TextWidget text(Supplier<Component> text) { return new TextWidget(text); }
    public static SignalText text(ReadableSignal<String> text) { return new SignalText(text); }
    public static TextWidget heading(String text) { return new TextWidget(text).style(TextStyle.HEADING); }
    public static TextWidget heading(Component text) { return new TextWidget(text).style(TextStyle.HEADING); }
    public static TextWidget title(String text) { return new TextWidget(text).style(TextStyle.TITLE); }
    public static TextWidget title(Component text) { return new TextWidget(text).style(TextStyle.TITLE); }

    public static ButtonWidget button(String label, Runnable action) { return new ButtonWidget(label).onPress(action).secondary(); }
    public static ButtonWidget button(Component label, Runnable action) { return new ButtonWidget(label).onPress(action).secondary(); }
    public static ButtonWidget button(Supplier<Component> label, Runnable action) { return new ButtonWidget(label).onPress(action).secondary(); }

    public static TextField textField(Signal<String> value) { return new TextField(value); }
    public static TextField textField(Signal<String> value, Font font) { return new TextField(value, font); }
    public static Checkbox checkbox(String label, Signal<Boolean> value) { return new Checkbox(label, value); }
    public static SwitchControl toggle(Signal<Boolean> value) { return new SwitchControl(value); }
    public static ProgressBar progress(ReadableSignal<? extends Number> value) { return new ProgressBar(value); }
    public static Slider slider(Signal<Double> value, double min, double max) { return new Slider(value, min, max); }
    public static <T> VirtualList<T> list(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) { return new VirtualList<>(items, renderer); }
    public static ScrollView scroll(UIComponent content) { return new ScrollView(content); }
    public static <T> DynamicGrid<T> grid(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) { return new DynamicGrid<>(items, renderer); }
    public static <T> VirtualGrid<T> virtualGrid(ReadableSignal<List<T>> items, Function<T, UIComponent> renderer) { return new VirtualGrid<>(items, renderer); }
    public static <T> SignalSwitcher<T> switcher(ReadableSignal<T> selection) { return new SignalSwitcher<>(selection); }
    public static <T> AsyncComponent<T> async(ReadableSignal<AsyncValue<T>> state, Supplier<UIComponent> loading,
                                               Function<T, UIComponent> success, Function<Throwable, UIComponent> error) {
        return new AsyncComponent<>(state, loading, success, error);
    }

    public static <T> Select<T> select(Signal<T> value) { return new Select<>(value); }
    public static Popover popover(UIComponent anchor, UIComponent content) { return new Popover(anchor, content); }
    public static Tooltip tooltip(String text) { return new Tooltip(text); }
    public static Tooltip tooltip(Component text) { return new Tooltip(text); }
    public static Tooltip tooltip(UIComponent content) { return new Tooltip(content); }
    public static ContextMenu contextMenu(int mouseX, int mouseY) { return new ContextMenu(mouseX, mouseY); }
    public static <T> Tabs<T> tabs(Signal<T> selection) { return new Tabs<>(selection); }
    public static <T> Table<T> table(ReadableSignal<List<T>> items) { return new Table<>(items); }
    public static <T> Radio<T> radio(T value, String label, Signal<T> signal) { return new Radio<>(value, label, signal); }
    public static <T> Radio<T> radio(T value, Component label, Signal<T> signal) { return new Radio<>(value, label, signal); }
    public static Badge badge(String label) { return new Badge(label); }
    public static Badge badge(Component label, Badge.Variant variant) { return new Badge(label, variant); }
    public static Chip chip(String label) { return new Chip(label); }
    public static Chip chip(Component label) { return new Chip(label); }
    public static Spinner spinner() { return new Spinner(); }
    public static Spinner spinner(int size) { return new Spinner(size); }
    public static Skeleton skeleton(int width, int height) { return new Skeleton(width, height); }
    public static LoadingOverlay loadingOverlay(UIComponent content) { return new LoadingOverlay(content); }
    public static EmptyState emptyState(String title) { return new EmptyState(title); }
    public static EmptyState emptyState(Component title) { return new EmptyState(title); }
    public static ErrorBoundary errorBoundary(Supplier<UIComponent> supplier) { return new ErrorBoundary(supplier); }
    public static LineChart lineChart() { return new LineChart(); }
    public static LineChart lineChart(List<Double> data) { return new LineChart(data); }
    public static com.nstut.openui.controls.AreaChart areaChart() { return new com.nstut.openui.controls.AreaChart(); }
    public static com.nstut.openui.controls.AreaChart areaChart(List<Double> data) { return new com.nstut.openui.controls.AreaChart(data); }
    public static BarChart barChart() { return new BarChart(); }
    public static Sparkline sparkline(List<Double> data) { return new Sparkline(data); }
    public static <T> com.nstut.openui.dnd.Draggable<T> draggable(T data, UIComponent child) { return new com.nstut.openui.dnd.Draggable<>(data, child); }
    public static <T> com.nstut.openui.dnd.DragTarget<T> dragTarget(Class<T> type, UIComponent child) { return new com.nstut.openui.dnd.DragTarget<>(type, child); }
    public static IconWidget icon(ItemStack item) { return IconWidget.of(item); }
    public static IconWidget icon(Identifier texture, int u, int v, int w, int h) { return IconWidget.of(texture, u, v, w, h); }
    public static IconWidget icon(String glyph, int color) { return IconWidget.of(glyph, color); }
}
