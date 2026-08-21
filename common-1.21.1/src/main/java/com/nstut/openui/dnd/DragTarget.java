package com.nstut.openui.dnd;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DragTarget<T> extends UIComponent {
    private final UIComponent child;
    private final Class<T> dataType;
    private Predicate<T> acceptPredicate = data -> true;
    private Consumer<DropEvent<T>> onDrop;
    private boolean dragOver;

    public DragTarget(Class<T> dataType, UIComponent child) {
        this.dataType = Objects.requireNonNull(dataType);
        this.child = Objects.requireNonNull(child);
        addChild(child);
    }

    public DragTarget<T> accept(Predicate<T> predicate) {
        this.acceptPredicate = Objects.requireNonNull(predicate);
        return this;
    }

    public DragTarget<T> onDrop(Consumer<DropEvent<T>> onDrop) {
        this.onDrop = onDrop;
        return this;
    }

    public boolean canAccept(Object data) {
        if (data != null && dataType.isInstance(data)) {
            return acceptPredicate.test(dataType.cast(data));
        }
        return false;
    }

    public void handleDrop(DropEvent<?> event) {
        if (canAccept(event.data()) && onDrop != null) {
            onDrop.accept(new DropEvent<>(dataType.cast(event.data()), event.source(), event.mouseX(), event.mouseY()));
        }
    }

    public boolean isDragOver() { return dragOver; }
    public void setDragOver(boolean dragOver) {
        this.dragOver = dragOver;
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

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        child.render(g, font, mx, my, pt);
        if (dragOver) {
            int highlight = theme().colors().primaryDim() | 0x40000000;
            g.fill(x, y, x + width, y + height, highlight);
        }
    }
}
