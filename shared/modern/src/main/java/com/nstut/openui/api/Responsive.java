package com.nstut.openui.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;
import java.util.function.Function;

public class Responsive extends UIComponent {
    public record Context(int width, int height) { }
    private final Function<Context, UIComponent> builder;
    private Context lastContext;

    public Responsive(Function<Context, UIComponent> builder) { this.builder = Objects.requireNonNull(builder); }
    @Override public int preferredWidth(Font font) { return children.isEmpty() ? 0 : children.get(0).preferredWidth(font); }
    @Override public int preferredHeight(Font font) { return children.isEmpty() ? 0 : children.get(0).preferredHeight(font); }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        Context context = new Context(availableWidth, availableHeight);
        if (!context.equals(lastContext)) {
            UIComponent built = Objects.requireNonNull(builder.apply(context));
            if (children.isEmpty()) addChild(built); else replaceChild(0, built);
            lastContext = context;
        }
        setBounds(x, y, availableWidth, availableHeight);
        children.get(0).layout(x, y, availableWidth, availableHeight);
    }

    @Override public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) { renderChildren(graphics, font, mouseX, mouseY, partialTick); }
}
