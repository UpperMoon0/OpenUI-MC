package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;

public abstract class Transition extends UIComponent {
    protected final UIComponent child;
    protected float progress = 1.0F;

    public Transition(UIComponent child) {
        this.child = Objects.requireNonNull(child);
        addChild(child);
    }

    public UIComponent getChild() { return child; }
    public float getProgress() { return progress; }
    public void setProgress(float progress) {
        this.progress = Math.max(0.0F, Math.min(1.0F, progress));
        invalidatePaint();
    }

    @Override
    public int preferredWidth(Font font) {
        return child.preferredWidth(font);
    }

    @Override
    public int preferredHeight(Font font) {
        return child.preferredHeight(font);
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        setBounds(lx, ly, availableWidth, availableHeight);
        child.layoutTree(measureFont(), lx, ly, availableWidth, availableHeight);
    }
}
