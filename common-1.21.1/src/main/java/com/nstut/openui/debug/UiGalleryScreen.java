package com.nstut.openui.debug;

import com.nstut.openui.api.ButtonWidget;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.Badge;
import com.nstut.openui.controls.Dialog;
import com.nstut.openui.controls.Select;
import com.nstut.openui.controls.Toast;
import com.nstut.openui.minecraft.UiScreen;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import com.nstut.openui.theme.TextStyle;
import com.nstut.openui.theme.Theme;
import net.minecraft.network.chat.Component;

import java.util.List;

public class UiGalleryScreen extends UiScreen {
    public enum Category { BUTTONS, INPUTS, DATA, CHARTS, FEEDBACK, THEME }

    private final Signal<Category> category = Signals.of(Category.BUTTONS);
    private final Signal<String> textInput = Signals.of("Hello OpenUI");
    private final Signal<Boolean> checkboxVal = Signals.of(true);
    private final Signal<Boolean> toggleVal = Signals.of(false);
    private final Signal<Double> sliderVal = Signals.of(0.65);
    private final Signal<String> selectVal = Signals.of("Gold");
    private final Signal<String> radioVal = Signals.of("Option A");

    public UiGalleryScreen() {
        super(Component.literal("OpenUI Component Gallery"));
    }

    @Override
    protected UIComponent buildUI() {
        return Ui.padding(10, Ui.row(
                sidebar(),
                content().flex()
        ).gap(10));
    }

    private UIComponent sidebar() {
        return Ui.card(
                Ui.column(
                        Ui.heading("Component Gallery"),
                        Ui.divider(),
                        navButton("Buttons", Category.BUTTONS),
                        navButton("Inputs", Category.INPUTS),
                        navButton("Data & Navigation", Category.DATA),
                        navButton("Charts", Category.CHARTS),
                        navButton("Feedback & States", Category.FEEDBACK),
                        navButton("Themes", Category.THEME),
                        Ui.spacer(),
                        Ui.button("Toggle Inspector (F8)", () -> UiInspector.toggle(uiRuntime())).small()
                ).gap(4)
        ).elevated(true).padding(8).width(130);
    }

    private UIComponent navButton(String label, Category target) {
        return Ui.button(label, () -> category.set(target))
                .variant(category.get() == target ? ButtonWidget.Variant.PRIMARY : ButtonWidget.Variant.GHOST)
                .alignLeft();
    }

    private UIComponent content() {
        return Ui.card(
                Ui.switcher(category)
                        .when(Category.BUTTONS, this::buttonsGallery)
                        .when(Category.INPUTS, this::inputsGallery)
                        .when(Category.DATA, this::dataGallery)
                        .when(Category.CHARTS, this::chartsGallery)
                        .when(Category.FEEDBACK, this::feedbackGallery)
                        .when(Category.THEME, this::themeGallery)
        ).padding(12).elevated(true);
    }

    private UIComponent buttonsGallery() {
        return Ui.column(
                Ui.title("Buttons & Variants"),
                Ui.text("Comprehensive button variants, sizes, and states:"),
                Ui.row(
                        Ui.button("Primary", () -> {}).primary(),
                        Ui.button("Secondary", () -> {}).secondary(),
                        Ui.button("Ghost", () -> {}).ghost(),
                        Ui.button("Danger", () -> {}).danger(),
                        Ui.button("Success", () -> {}).success(),
                        Ui.button("Outline", () -> {}).outline()
                ).gap(6),
                Ui.heading("Sizes"),
                Ui.row(
                        Ui.button("Small", () -> {}).small().primary(),
                        Ui.button("Medium", () -> {}).medium().primary(),
                        Ui.button("Large", () -> {}).large().primary()
                ).gap(6),
                Ui.heading("States"),
                Ui.row(
                        Ui.button("Disabled", () -> {}).enabled(false),
                        Ui.button("Active Tab", () -> {}).activeIndicator()
                ).gap(6)
        ).gap(8);
    }

    private UIComponent inputsGallery() {
        return Ui.column(
                Ui.title("Form Controls & Inputs"),
                Ui.row(
                        Ui.text("TextField:"),
                        Ui.textField(textInput).placeholder("Type here...").width(120)
                ).gap(8),
                Ui.row(
                        Ui.checkbox("Checkbox", checkboxVal),
                        Ui.toggle(toggleVal)
                ).gap(12),
                Ui.row(
                        Ui.text("Slider:"),
                        Ui.slider(sliderVal, 0.0, 1.0).width(100),
                        Ui.progress(sliderVal).width(60)
                ).gap(8),
                Ui.row(
                        Ui.radio("Option A", "Option A", radioVal),
                        Ui.radio("Option B", "Option B", radioVal)
                ).gap(12)
        ).gap(8);
    }

    private UIComponent dataGallery() {
        Signal<String> activeTab = Signals.of("Browse");
        return Ui.column(
                Ui.title("Data & Navigation Controls"),
                Ui.heading("Tabs"),
                Ui.tabs(activeTab)
                        .tab("Browse", "Browse")
                        .tab("Orders", "Orders")
                        .tab("Portfolio", "Portfolio"),
                Ui.heading("Select Dropdown"),
                Ui.select(selectVal)
                        .option("Iron Ingot", "Iron")
                        .option("Gold Ingot", "Gold")
                        .option("Diamond", "Diamond")
                        .width(130),
                Ui.heading("Dialog & Modal"),
                Ui.row(
                        Ui.button("Open Confirm Dialog", () -> {
                            Dialog.confirm(uiRuntime().overlays(), "Confirm Action", "Do you want to proceed with this action?", () -> {
                                Toast.show(uiRuntime().overlays(), Toast.success("Confirmed", "Action executed successfully!"));
                            }, () -> {});
                        }).primary()
                ).gap(6)
        ).gap(8);
    }

    private UIComponent chartsGallery() {
        return Ui.column(
                Ui.title("Data Visualization & Charts"),
                Ui.row(
                        Ui.column(
                                Ui.heading("Line Chart"),
                                Ui.lineChart(List.of(12.0, 18.5, 14.2, 28.0, 22.1, 35.0, 31.2)).width(150).height(80)
                        ).gap(4),
                        Ui.column(
                                Ui.heading("Bar Chart"),
                                Ui.barChart().bar("Mon", 20).bar("Tue", 45).bar("Wed", 30).bar("Thu", 60).width(150).height(80)
                        ).gap(4)
                ).gap(10)
        ).gap(8);
    }

    private UIComponent feedbackGallery() {
        return Ui.column(
                Ui.title("Feedback & Loading Primitives"),
                Ui.row(
                        Ui.badge("NEW"),
                        Ui.badge(Component.literal("99+"), Badge.Variant.DANGER),
                        Ui.chip("Filter: Active"),
                        Ui.spinner(16),
                        Ui.skeleton(60, 14)
                ).gap(8),
                Ui.row(
                        Ui.button("Show Toast", () -> {
                            Toast.show(uiRuntime().overlays(), Toast.info("Notification", "This is a toast notification."));
                        }).small()
                ).gap(6)
        ).gap(8);
    }

    private UIComponent themeGallery() {
        return Ui.column(
                Ui.title("Theme 2.0 System"),
                Ui.text("Switch active framework theme runtime:"),
                Ui.row(
                        Ui.button("Dark Theme", () -> uiRuntime().theme(Theme.dark())),
                        Ui.button("Light Theme", () -> uiRuntime().theme(Theme.light())),
                        Ui.button("High Contrast", () -> uiRuntime().theme(Theme.highContrast()))
                ).gap(6)
        ).gap(8);
    }
}
