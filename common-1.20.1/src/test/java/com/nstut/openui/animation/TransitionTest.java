package com.nstut.openui.animation;

import com.nstut.openui.api.Ui;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransitionTest {

    @Test
    void transitionsUpdateProgressAndPreserveChildLayout() {
        SlideTransition slide = new SlideTransition(Ui.text("Hello"), SlideTransition.Direction.RIGHT, 30);
        slide.layout(0, 0, 100, 20);

        assertEquals(1.0F, slide.getProgress());
        assertEquals(100, slide.getWidth());
        assertEquals(20, slide.getHeight());
        assertEquals(100, slide.getChild().getWidth());

        slide.setProgress(0.5F);
        assertEquals(0.5F, slide.getProgress());
    }
}
