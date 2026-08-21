package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.state.Signal;
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
        if (Objects.equals(this.selectedValue, val)) return;
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
            maxW = Math.max(maxW, font.width(opt.label()));
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
        if (selected != null) {
            int textX = x + 8;
            if (selected.icon() != null) {
                selected.icon().layout(textX, y + (height - 16) / 2, 16, 16);
                selected.icon().render(g, font, mx, my, pt);
                textX += 20;
            }
            g.drawString(font, selected.label(), textX, y + (height - font.lineHeight) / 2, colors.onSurface());
        }

        // Arrow indicator
        String arrow = open ? "▲" : "▼";
        int arrowW = font.width(arrow);
        g.drawString(font, arrow, x + width - arrowW - 8, y + (height - font.lineHeight) / 2, colors.onSurfaceMuted());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && isHovered()) {
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
        private int hoveredIndex = -1;

        private DropdownMenu() {
            focusable(true);
        }

        @Override
        public int preferredWidth(Font font) {
            return Select.this.width;
        }

        @Override
        public int preferredHeight(Font font) {
            int itemH = 16;
            return Math.min(160, options.size() * itemH + 6);
        }

        @Override
        public void layout(int lx, int ly, int availableWidth, int availableHeight) {
            int menuH = preferredHeight(measureFont());
            int menuY = Select.this.y + Select.this.height + 2;
            int menuW = Select.this.width;
            int menuX = Select.this.x;

            if (runtime() != null && menuY + menuH > runtime().root().getHeight()) {
                menuY = Math.max(2, Select.this.y - menuH - 2);
            }
            setBounds(menuX, menuY, menuW, menuH);
        }

        @Override
        public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
            Theme t = theme();
            ColorScheme colors = t.colors();

            UiRender.shadow(g, x, y, width, height, t.cardTheme().radius());
            UiRender.roundedOutline(g, x, y, width, height, t.cardTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

            int itemH = 16;
            int curY = y + 3;
            T curVal = getValue();

            for (int i = 0; i < options.size(); i++) {
                Option<T> opt = options.get(i);
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

                int textCol = isSelected ? colors.primaryHover() : colors.onSurface();
                g.drawString(font, opt.label(), textX, curY + (itemH - font.lineHeight) / 2, textCol);
                curY += itemH;
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (btn == 0 && mx >= x && mx < x + width) {
                int itemH = 16;
                int clickedIdx = (int) ((my - (y + 3)) / itemH);
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
