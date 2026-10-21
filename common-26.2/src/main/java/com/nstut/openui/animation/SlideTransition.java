package com.nstut.openui.animation;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SlideTransition extends Transition {
    public enum Direction { LEFT, RIGHT, UP, DOWN }

    private final Direction direction;
    private final int distance;

    public SlideTransition(UIComponent child, Direction direction, int distance) {
        super(child);
        this.direction = direction != null ? direction : Direction.RIGHT;
        this.distance = distance;
    }

    public SlideTransition(UIComponent child, Direction direction) {
        this(child, direction, 20);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        float offsetFactor = 1.0F - progress;
        int dx = 0;
        int dy = 0;

        switch (direction) {
            case LEFT -> dx = Math.round(distance * offsetFactor);
            case RIGHT -> dx = -Math.round(distance * offsetFactor);
            case UP -> dy = Math.round(distance * offsetFactor);
            case DOWN -> dy = -Math.round(distance * offsetFactor);
        }

        g.pose().pushMatrix();
        g.pose().translate(dx, dy);
        try {
            child.render(g, font, mx, my, pt);
        } finally {
            g.pose().popMatrix();
        }
    }
}
