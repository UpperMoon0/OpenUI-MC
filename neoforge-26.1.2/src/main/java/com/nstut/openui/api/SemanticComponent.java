package com.nstut.openui.api;

import com.nstut.openui.semantics.Semantics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

/** Transparent wrapper that attaches accessibility semantics to retained UI. */
public final class SemanticComponent extends UIComponent {
    private final UIComponent child;
    private Semantics semantics;

    public SemanticComponent(Semantics semantics, UIComponent child) {
        this.semantics = Objects.requireNonNull(semantics, "semantics");
        this.child = Objects.requireNonNull(child, "child");
        addChild(child);
    }

    public Semantics semantics() { return semantics; }
    public SemanticComponent semantics(Semantics value) { semantics = Objects.requireNonNull(value); return this; }
    @Override public int preferredWidth(Font font) { return child.preferredWidth(font); }
    @Override public int preferredHeight(Font font) { return child.preferredHeight(font); }
    @Override public void layout(int x, int y, int w, int h) { setBounds(x, y, w, h); child.layout(x, y, w, h); }
    @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) { child.render(g, font, mx, my, pt); }
}
