package com.nstut.openui.controls;

import com.nstut.openui.animation.Easing;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Subscription;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Tabs<T> extends UIComponent {
    public record TabItem<T>(T value, Component label, IconWidget icon) {
        public TabItem(T value, String label) {
            this(value, Component.literal(label), null);
        }
        public TabItem(T value, Component label) {
            this(value, label, null);
        }
    }

    private final List<TabItem<T>> tabs = new ArrayList<>();
    private final Signal<T> selectedSignal;
    private T selectedValue;
    private Consumer<T> onTabChanged;
    private Subscription subscription = Subscription.EMPTY;

    // Indicator animation state
    private float indicatorX;
    private float indicatorW;

    public Tabs(Signal<T> selection) {
        this.selectedSignal = Objects.requireNonNull(selection);
        this.selectedValue = selection.get();
        focusable(true);
    }

    @Override
    protected void onMount() {
        if (selectedSignal != null) {
            subscription = selectedSignal.subscribe(val -> {
                if (!Objects.equals(this.selectedValue, val)) {
                    this.selectedValue = val;
                    animateToSelected();
                }
            });
        }
    }

    @Override
    protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
    }

    public Tabs<T> tab(T value, String label) {
        tabs.add(new TabItem<>(value, label));
        if (selectedValue == null) selectedValue = value;
        invalidateLayout();
        return this;
    }

    public Tabs<T> tab(T value, Component label) {
        tabs.add(new TabItem<>(value, label));
        if (selectedValue == null) selectedValue = value;
        invalidateLayout();
        return this;
    }

    public Tabs<T> tab(T value, Component label, IconWidget icon) {
        tabs.add(new TabItem<>(value, label, icon));
        if (selectedValue == null) selectedValue = value;
        invalidateLayout();
        return this;
    }

    public Tabs<T> onChange(Consumer<T> onTabChanged) {
        this.onTabChanged = onTabChanged;
        return this;
    }

    public void select(T val) {
        if (Objects.equals(selected(), val)) return;
        this.selectedValue = val;
        if (selectedSignal != null) selectedSignal.set(val);
        if (onTabChanged != null) onTabChanged.accept(val);
        animateToSelected();
    }

    private void animateToSelected() {
        if (tabs.isEmpty() || width <= 0) return;
        int tabCount = Math.max(1, tabs.size());
        int tabW = width / tabCount;
        indicatorW = tabW - 4;
        float targetIndicatorX = x + findSelectedIndex() * tabW + 2;

        if (runtime() != null && !theme().reducedMotion()) {
            runtime().animations().animateFloat(indicatorX, targetIndicatorX, theme().durations().hoverMs(), Easing.EASE_OUT, v -> {
                indicatorX = v;
                invalidatePaint();
            });
        } else {
            indicatorX = targetIndicatorX;
            invalidatePaint();
        }
    }

    public T selected() {
        return selectedSignal != null ? selectedSignal.get() : selectedValue;
    }

    private int findSelectedIndex() {
        T cur = selected();
        for (int i = 0; i < tabs.size(); i++) {
            if (Objects.equals(tabs.get(i).value(), cur)) return i;
        }
        return 0;
    }

    @Override
    public int preferredWidth(Font font) {
        if (font == null) return tabs.size() * 60;
        int totalW = 0;
        for (TabItem<T> tab : tabs) {
            totalW += font.width(tab.label()) + 24;
        }
        return Math.max(120, totalW);
    }

    @Override
    public int preferredHeight(Font font) {
        return 22;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        int tabCount = Math.max(1, tabs.size());
        int tabW = availableWidth / tabCount;
        indicatorW = tabW - 4;
        if (indicatorX <= 0 || theme().reducedMotion()) {
            indicatorX = lx + findSelectedIndex() * tabW + 2;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible || tabs.isEmpty()) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.roundedOutline(g, x, y, width, height, t.radii().small(), colors.surface(), colors.borderSubtle());

        int tabCount = tabs.size();
        int tabW = width / tabCount;
        int selectedIdx = findSelectedIndex();

        if (t.reducedMotion() || indicatorX <= 0) {
            indicatorX = x + selectedIdx * tabW + 2;
            indicatorW = tabW - 4;
        }

        // Draw indicator pill
        UiRender.roundedRect(g, Math.round(indicatorX), y + 2, Math.round(indicatorW), height - 4, t.radii().small() - 1, colors.surfaceVariant());

        for (int i = 0; i < tabCount; i++) {
            TabItem<T> tab = tabs.get(i);
            int curX = x + i * tabW;
            boolean isSelected = (i == selectedIdx);
            int textCol = isSelected ? colors.primary() : colors.onSurfaceMuted();

            int textW = font != null ? font.width(tab.label()) : tab.label().getString().length() * 6;
            int textX = curX + (tabW - textW) / 2;
            int textY = y + (height - (font != null ? font.lineHeight : 9)) / 2;

            if (tab.icon() != null) {
                tab.icon().layout(textX - 16, y + (height - 12) / 2, 12, 12);
                tab.icon().render(g, font, mx, my, pt);
            }

            if (font != null) {
                g.text(font, tab.label(), textX, textY, textCol);
            }
        }

        if (isFocused()) {
            UiRender.roundedOutline(g, x, y, width, height, t.radii().small(), 0, colors.primary());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= x && mx < x + width && my >= y && my < y + height && !tabs.isEmpty()) {
            int tabCount = tabs.size();
            int tabW = width / tabCount;
            int clickedIdx = (int) ((mx - x) / tabW);
            if (clickedIdx >= 0 && clickedIdx < tabs.size()) {
                select(tabs.get(clickedIdx).value());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 263) { // Left
            int idx = findSelectedIndex();
            if (idx - 1 >= 0) select(tabs.get(idx - 1).value());
            return true;
        }
        if (key == 262) { // Right
            int idx = findSelectedIndex();
            if (idx + 1 < tabs.size()) select(tabs.get(idx + 1).value());
            return true;
        }
        return false;
    }
}
