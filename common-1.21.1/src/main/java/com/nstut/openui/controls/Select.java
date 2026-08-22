package com.nstut.openui.controls;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Subscription;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class Select<T> extends UIComponent {
    public record Option<T>(Component label, T value, IconWidget icon) {
        public Option(String label, T value) {
            this(Component.literal(label), value, null);
        }
        public Option(Component label, T value) {
            this(label, value, null);
        }
    }

    private final List<Option<T>> options = new ArrayList<>();
    private Signal<T> signal;
    private T selectedValue;
    private Consumer<T> onChange;
    private OverlayHandle activeDropdown;
    private Subscription subscription = Subscription.EMPTY;
    private boolean open;
    private int customWidth = -1;

    public Select() {
        focusable(true);
    }

    public Select(Signal<T> signal) {
        this();
        this.signal = signal;
        if (signal != null) {
            this.selectedValue = signal.get();
        }
    }

    @Override
    protected void onMount() {
        if (signal != null) {
            subscription = signal.subscribe(val -> {
                this.selectedValue = val;
                invalidatePaint();
            });
        }
    }

    @Override
    protected void onUnmount() {
        subscription.close();
        subscription = Subscription.EMPTY;
        closeDropdown();
    }

    public Select<T> option(String label, T value) {
        options.add(new Option<>(label, value));
        if (selectedValue == null && !options.isEmpty()) selectedValue = options.get(0).value();
        invalidateLayout();
        return this;
    }

    public Select<T> option(Component label, T value) {
        options.add(new Option<>(label, value));
        if (selectedValue == null && !options.isEmpty()) selectedValue = options.get(0).value();
        invalidateLayout();
        return this;
    }

    public Select<T> option(Component label, T value, IconWidget icon) {
        options.add(new Option<>(label, value, icon));
        if (selectedValue == null && !options.isEmpty()) selectedValue = options.get(0).value();
        invalidateLayout();
        return this;
    }

    public Select<T> width(int width) {
        this.customWidth = Math.max(20, width);
        invalidateLayout();
        return this;
    }

    public Select<T> onChange(Consumer<T> onChange) {
        this.onChange = onChange;
        return this;
    }

    public T getValue() {
        return signal != null ? signal.get() : selectedValue;
    }

    public void setValue(T val) {
        if (Objects.equals(getValue(), val)) return;
        this.selectedValue = val;
        if (signal != null) signal.set(val);
        if (onChange != null) onChange.accept(val);
        invalidatePaint();
    }

    private Option<T> findSelectedOption() {
        T current = getValue();
        for (Option<T> opt : options) {
            if (Objects.equals(opt.value(), current)) return opt;
        }
        return options.isEmpty() ? null : options.get(0);
    }

    public void toggleDropdown() {
        if (open) closeDropdown();
        else openDropdown();
    }

    public void openDropdown() {
        if (open || runtime() == null || options.isEmpty()) return;
        open = true;

        DropdownMenu menu = new DropdownMenu();
        activeDropdown = runtime().overlays().show(
                OverlayLayer.DROPDOWN,
                menu,
                false,
                true,
                true,
                () -> {
                    open = false;
                    activeDropdown = null;
                    invalidatePaint();
                }
        );
        invalidatePaint();
    }

    public void closeDropdown() {
        if (activeDropdown != null) {
            activeDropdown.close();
            activeDropdown = null;
        }
        open = false;
        invalidatePaint();
    }

    @Override
    public int preferredWidth(Font font) {
        if (customWidth > 0) return customWidth;
        if (font == null) return 80;
        int maxW = 0;
        for (Option<T> opt : options) {
            int w = 0;
            try {
                w = font.width(opt.label());
            } catch (Throwable ignored) {
                w = opt.label().getString().length() * 6;
            }
            maxW = Math.max(maxW, w);
        }
        return maxW + 28;
    }

    @Override
    public int preferredHeight(Font font) {
        return theme().buttonTheme().heightMd();
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        int radius = t.buttonTheme().radius();
        int bg = isHovered() || open ? colors.surfaceVariant() : colors.surfaceRaised();
        int border = isFocused() || open ? colors.primary() : (isHovered() ? colors.borderStrong() : colors.border());

        UiRender.roundedOutline(g, x, y, width, height, radius, bg, border);

        Option<T> selected = findSelectedOption();
        int textH = font != null ? font.lineHeight : 9;
        if (selected != null) {
            int textX = x + 8;
            if (selected.icon() != null) {
                selected.icon().layout(textX, y + (height - 16) / 2, 16, 16);
                selected.icon().render(g, font, mx, my, pt);
                textX += 20;
            }
            if (font != null) {
                int arrowSpace = font.width("▼") + 16;
                int available = Math.max(1, x + width - arrowSpace - textX);
                // Pixel-safe ellipsis currently flattens styled component segments.
                String fullLabel = selected.label().getString();
                String label = font.width(fullLabel) <= available
                        ? fullLabel
                        : font.plainSubstrByWidth(fullLabel, Math.max(1, available - font.width("..."))) + "...";
                UiRender.text(g, font, label, textX, y + (height - textH) / 2, colors.onSurface());
            }
        }

        // Arrow indicator
        String arrow = open ? "▲" : "▼";
        int arrowW = font != null ? font.width(arrow) : 6;
        if (font != null) {
            UiRender.text(g, font, arrow, x + width - arrowW - 8, y + (height - textH) / 2, colors.onSurfaceMuted());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= x && mx < x + width && my >= y && my < y + height) {
            toggleDropdown();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 257 || key == 32) { // Enter or Space
            toggleDropdown();
            return true;
        }
        if (key == 264) { // Down
            int idx = findSelectedIndex();
            if (idx + 1 < options.size()) setValue(options.get(idx + 1).value());
            return true;
        }
        if (key == 265) { // Up
            int idx = findSelectedIndex();
            if (idx - 1 >= 0) setValue(options.get(idx - 1).value());
            return true;
        }
        return false;
    }

    private int findSelectedIndex() {
        T current = getValue();
        for (int i = 0; i < options.size(); i++) {
            if (Objects.equals(options.get(i).value(), current)) return i;
        }
        return -1;
    }

    private final class DropdownMenu extends UIComponent {
        private int scrollOffset = 0;
        private final int itemH = 16;

        private DropdownMenu() {
            focusable(true);
        }

        @Override
        public int preferredWidth(Font font) {
            return Select.this.width;
        }

        @Override
        public int preferredHeight(Font font) {
            return Math.min(160, options.size() * itemH + 6);
        }

        @Override
        public void layout(int lx, int ly, int availableWidth, int availableHeight) {
            int menuH = preferredHeight(measureFont());
            int menuY = Select.this.y + Select.this.height + 2;
            int menuW = Select.this.width;
            int menuX = Select.this.x;

            if (menuY + menuH > ly + availableHeight) {
                menuY = Math.max(ly + 2, Select.this.y - menuH - 2);
            }
            setBounds(menuX, menuY, menuW, menuH);
        }

        @Override
        public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
            Theme t = theme();
            ColorScheme colors = t.colors();

            UiRender.shadow(g, x, y, width, height, t.cardTheme().radius());
            UiRender.roundedOutline(g, x, y, width, height, t.cardTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

            int maxVisible = Math.max(1, (height - 6) / itemH);
            int maxScroll = Math.max(0, options.size() - maxVisible);
            scrollOffset = Math.min(scrollOffset, maxScroll);

            int startIdx = scrollOffset;
            int endIdx = Math.min(options.size(), startIdx + maxVisible + 1);

            ClipStack.push(g, x + 2, y + 2, width - 4, height - 4);
            try {
                T curVal = getValue();
                int textH = font != null ? font.lineHeight : 9;

                for (int i = startIdx; i < endIdx; i++) {
                    Option<T> opt = options.get(i);
                    int curY = y + 3 + (i - startIdx) * itemH;
                    if (curY + itemH > y + height - 1) break;

                    boolean isSelected = Objects.equals(opt.value(), curVal);
                    boolean isItemHovered = mx >= x + 2 && mx < x + width - 2 && my >= curY && my < curY + itemH;

                    if (isItemHovered || isSelected) {
                        int itemBg = isSelected ? colors.primaryDim() : colors.surfaceVariant();
                        UiRender.roundedRect(g, x + 3, curY, width - 6, itemH, 2, itemBg);
                    }

                    int textX = x + 8;
                    if (opt.icon() != null) {
                        opt.icon().layout(textX, curY + (itemH - 12) / 2, 12, 12);
                        opt.icon().render(g, font, mx, my, pt);
                        textX += 16;
                    }

                    int textCol = isSelected ? colors.onPrimary() : colors.onSurface();
                    if (font != null) {
                        UiRender.text(g, font, opt.label(), textX, curY + (itemH - textH) / 2, textCol);
                    }
                }
            } finally {
                ClipStack.pop(g);
            }
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if (delta != 0) {
                int maxVisible = Math.max(1, (height - 6) / itemH);
                int maxScroll = Math.max(0, options.size() - maxVisible);
                scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
                invalidatePaint();
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0 && mx >= x && mx < x + width && my >= y && my < y + height) {
                int clickedIdx = scrollOffset + (int) ((my - (y + 3)) / itemH);
                if (clickedIdx >= 0 && clickedIdx < options.size()) {
                    setValue(options.get(clickedIdx).value());
                    closeDropdown();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean keyPressed(int key, int scanCode, int modifiers) {
            if (key == 256) { // Escape
                closeDropdown();
                return true;
            }
            return false;
        }
    }
}
