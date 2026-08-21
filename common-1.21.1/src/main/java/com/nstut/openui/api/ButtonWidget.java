package com.nstut.openui.api;

import com.nstut.openui.animation.Easing;
import com.nstut.openui.input.EventType;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Supplier;

public class ButtonWidget extends UIComponent {

    public enum Variant { PRIMARY, SECONDARY, GHOST, DANGER, SUCCESS, OUTLINE }
    public enum Size { SMALL, MEDIUM, LARGE }

    private Supplier<Component> labelSupplier;
    private Variant variant = Variant.SECONDARY;
    private Size size = Size.MEDIUM;
    private boolean active;
    private boolean enabled = true;
    private boolean alignLeft;
    private boolean activeIndicator;
    private int customRadius = -1;
    private int customHeight = -1;
    private Integer customNormalColor;
    private Integer customHoverColor;
    private Integer customTextColor;
    private Runnable onClick;

    // Time-based animation state
    private float currentHoverProgress;

    public ButtonWidget(String label, int normalColor, int hoverColor, int textColor) {
        this(Component.literal(label != null ? label : ""));
        this.customNormalColor = normalColor;
        this.customHoverColor = hoverColor;
        this.customTextColor = textColor;
    }

    public ButtonWidget(String label) {
        this(Component.literal(label != null ? label : ""));
    }

    public ButtonWidget(Component label) {
        this(() -> label);
    }

    public ButtonWidget(Supplier<Component> labelSupplier) {
        this.labelSupplier = Objects.requireNonNull(labelSupplier);
        focusable(true);
    }

    public ButtonWidget onPress(Runnable r) { this.onClick = r; return this; }
    public void setActive(boolean a) { this.active = a; invalidatePaint(); }
    public void setLabel(String label) { setLabel(Component.literal(label != null ? label : "")); }
    public void setLabel(Component label) { this.labelSupplier = () -> label; invalidateLayout(); }
    public void setLabel(Supplier<Component> supplier) { this.labelSupplier = Objects.requireNonNull(supplier); invalidateLayout(); }
    public ButtonWidget alignLeft() { this.alignLeft = true; return this; }
    public ButtonWidget activeIndicator() { this.activeIndicator = true; return this; }
    public ButtonWidget radius(int radius) { this.customRadius = Math.max(0, radius); return this; }
    public ButtonWidget height(int height) { this.customHeight = Math.max(0, height); return this; }
    public ButtonWidget textColor(int color) { this.customTextColor = color; return this; }
    public ButtonWidget enabled(boolean enabled) { this.enabled = enabled; return this; }
    public boolean isEnabled() { return enabled; }

    public ButtonWidget size(Size size) { this.size = Objects.requireNonNull(size); invalidateLayout(); return this; }
    public ButtonWidget small() { return size(Size.SMALL); }
    public ButtonWidget medium() { return size(Size.MEDIUM); }
    public ButtonWidget large() { return size(Size.LARGE); }

    public ButtonWidget primary() { return variant(Variant.PRIMARY); }
    public ButtonWidget secondary() { return variant(Variant.SECONDARY); }
    public ButtonWidget ghost() { return variant(Variant.GHOST); }
    public ButtonWidget danger() { return variant(Variant.DANGER); }
    public ButtonWidget success() { return variant(Variant.SUCCESS); }
    public ButtonWidget outline() { return variant(Variant.OUTLINE); }

    public ButtonWidget variant(Variant variant) {
        this.variant = Objects.requireNonNull(variant);
        invalidatePaint();
        return this;
    }

    public void setColors(int normalColor, int hoverColor) {
        this.customNormalColor = normalColor;
        this.customHoverColor = hoverColor;
        invalidatePaint();
    }

    public Component getLabel() {
        return labelSupplier != null ? labelSupplier.get() : Component.empty();
    }

    @Override
    public int preferredWidth(Font font) {
        if (font == null) return 60;
        int basePadding = switch (size) {
            case SMALL -> 8;
            case MEDIUM -> 16;
            case LARGE -> 24;
        };
        int textWidth = 40;
        try {
            textWidth = font.width(getLabel());
        } catch (Throwable ignored) {
            textWidth = getLabel().getString().length() * 6;
        }
        return Math.max(40, textWidth + basePadding);
    }

    @Override
    public int preferredHeight(Font font) {
        if (customHeight > 0) return customHeight;
        Theme t = theme();
        int baseHeight = switch (size) {
            case SMALL -> t.buttonTheme().heightSm();
            case LARGE -> t.buttonTheme().heightLg();
            case MEDIUM -> t.buttonTheme().heightMd();
        };
        if (getLabel().getString().contains("\n")) {
            return 22;
        }
        return baseHeight;
    }

    @Override
    protected void onMount() {
        on(EventType.HOVER_ENTER, e -> {
            if (runtime() != null && enabled && !theme().reducedMotion()) {
                runtime().animations().animateFloat(currentHoverProgress, 1.0F, theme().durations().hoverMs(), Easing.EASE_OUT, v -> {
                    currentHoverProgress = v;
                    invalidatePaint();
                });
            } else {
                currentHoverProgress = enabled ? 1.0F : 0.0F;
                invalidatePaint();
            }
        });
        on(EventType.HOVER_LEAVE, e -> {
            if (runtime() != null && !theme().reducedMotion()) {
                runtime().animations().animateFloat(currentHoverProgress, 0.0F, theme().durations().hoverMs(), Easing.EASE_OUT, v -> {
                    currentHoverProgress = v;
                    invalidatePaint();
                });
            } else {
                currentHoverProgress = 0.0F;
                invalidatePaint();
            }
        });
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        if (t.reducedMotion()) {
            currentHoverProgress = (enabled && isHovered()) ? 1.0F : 0.0F;
        }

        float eased = Easing.EASE_OUT.apply(currentHoverProgress);

        int normalCol = customNormalColor != null ? customNormalColor : switch (variant) {
            case PRIMARY -> colors.primaryDim();
            case SECONDARY -> colors.surfaceRaised();
            case GHOST -> 0x00000000;
            case DANGER -> colors.dangerDeep();
            case SUCCESS -> colors.successDeep();
            case OUTLINE -> 0x00000000;
        };

        int hoverCol = customHoverColor != null ? customHoverColor : switch (variant) {
            case PRIMARY -> colors.primary();
            case SECONDARY -> colors.surfaceVariant();
            case GHOST -> colors.surfaceVariant();
            case DANGER -> colors.danger();
            case SUCCESS -> colors.success();
            case OUTLINE -> colors.surfaceRaised();
        };

        int textCol = customTextColor != null ? customTextColor : switch (variant) {
            case PRIMARY -> colors.onPrimary();
            case SECONDARY -> colors.onSurface();
            case GHOST -> colors.onSurface();
            case DANGER -> colors.onPrimary();
            case SUCCESS -> colors.onPrimary();
            case OUTLINE -> colors.onSurface();
        };

        int fillColor = active ? hoverCol : UiRender.mix(normalCol, hoverCol, eased);
        if (!enabled) fillColor = UiRender.mix(fillColor, colors.shell(), 0.58F);

        int outline = active ? colors.primaryDim()
                : (variant == Variant.OUTLINE ? UiRender.mix(colors.border(), colors.borderStrong(), eased)
                : UiRender.mix(colors.borderSubtle(), colors.borderStrong(), eased));

        if (isFocused()) {
            outline = colors.primary();
        }

        int btnRadius = customRadius >= 0 ? customRadius : t.buttonTheme().radius();
        UiRender.roundedOutline(g, x, y, width, height, btnRadius, fillColor, outline);

        if (active && activeIndicator) {
            UiRender.roundedRect(g, x + 2, y + 3, 2, Math.max(2, height - 6), 1, colors.primary());
        }

        Component comp = getLabel();
        String str = comp.getString();
        String[] lines = str.split("\n", -1);
        int finalTextColor = enabled ? textCol : colors.onSurfaceDisabled();

        if (lines.length >= 2) {
            int totalTextHeight = font.lineHeight * 2;
            int firstY = y + (height - totalTextHeight) / 2;
            int firstWidth = font.width(lines[0]);
            int secondWidth = font.width(lines[1]);
            int firstX = alignLeft ? x + 8 : x + (width - firstWidth) / 2;
            int secondX = alignLeft ? x + 8 : x + (width - secondWidth) / 2;
            g.drawString(font, lines[0], firstX, firstY, colors.onSurfaceMuted());
            g.drawString(font, lines[1], secondX, firstY + font.lineHeight, finalTextColor);
        } else {
            int tw = font.width(comp);
            int ty = y + (height - font.lineHeight) / 2;
            int tx = alignLeft ? x + (activeIndicator ? 9 : 7) : x + (width - tw) / 2;
            g.drawString(font, comp, tx, ty, finalTextColor);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        boolean inBounds = visible && mx >= x && mx < x + width && my >= y && my < y + height;
        if (enabled && btn == 0 && inBounds && onClick != null) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (enabled && (key == 257 || key == 32) && onClick != null) {
            onClick.run();
            return true;
        }
        return false;
    }
}
