package com.nstut.openui.api;

import com.nstut.openui.context.ContextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;
import java.util.Optional;

/** Transparent retained component that provides one typed context value. */
public final class ContextProvider extends UIComponent {
    private final ContextKey<?> contextKey;
    private Object value;
    private final UIComponent child;

    public <T> ContextProvider(ContextKey<T> key, T value, UIComponent child) {
        this.contextKey = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
        this.child = Objects.requireNonNull(child, "child");
        addChild(child);
    }

    public <T> ContextProvider value(ContextKey<T> key, T value) {
        if (contextKey != key) throw new IllegalArgumentException("Context key does not match provider");
        this.value = Objects.requireNonNull(value, "value");
        invalidateBuild();
        return this;
    }

    @SuppressWarnings("unchecked")
    <T> Optional<T> findLocal(ContextKey<T> key) {
        return contextKey == key ? Optional.of((T) value) : Optional.empty();
    }

    @Override public int preferredWidth(Font font) { return child.preferredWidth(font); }
    @Override public int preferredHeight(Font font) { return child.preferredHeight(font); }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        child.layout(x, y, availableWidth, availableHeight);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        child.render(g, font, mx, my, pt);
    }
}
