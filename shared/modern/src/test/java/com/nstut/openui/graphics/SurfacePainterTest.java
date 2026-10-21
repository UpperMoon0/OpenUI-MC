package com.nstut.openui.graphics;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SurfacePainterTest {
    @Test void translucentSurfaceNeverPaintsAPixelTwiceOrOutsideBounds() {
        for (int w = 1; w <= 32; w++) for (int h = 1; h <= 32; h++) {
            int[][] hits = new int[w][h];
            final int width = w, height = h;
            SurfacePainter.paint((x, y, rw, rh, color) -> {
                assertTrue(rw >= 0 && rh > 0);
                assertTrue(x >= 0 && y >= 0 && x + rw <= width && y + rh <= height);
                for (int xx = x; xx < x + rw; xx++) for (int yy = y; yy < y + rh; yy++)
                    assertEquals(1, ++hits[xx][yy], "Alpha overdraw");
            }, 0, 0, w, h, new SurfaceStyle(0x80224433, 0x80112211, 0xAABBCCDD, 10, 0, 0));
            assertEquals(1, hits[w / 2][h / 2], "Missing interior");
        }
    }

    @Test void gradientPreservesAlphaAndBorderDoesNotUnderpaintInterior() {
        int[][] colors = new int[12][12];
        SurfacePainter.paint((x,y,w,h,c) -> {
            for (int xx=x;xx<x+w;xx++) for(int yy=y;yy<y+h;yy++) colors[xx][yy]=c;
        },0,0,12,12,new SurfaceStyle(0x80408040,0x80204020,0xDDFFFFFF,0,0,0));
        assertEquals(0xDDFFFFFF, colors[0][6]);
        assertEquals(0x80, colors[6][6] >>> 24);
        assertNotEquals(colors[6][2], colors[6][9]);
    }

    @Test void emptyBoundsDoNotDrawAndShadowCostIsBounded() {
        SurfacePainter.paint((x,y,w,h,c) -> fail("Empty bounds"),0,0,0,20,
                new SurfaceStyle(0,0,0,10,200,0));
        assertEquals(16, new SurfaceStyle(0,0,0,-1,200,0).shadowExtent());
        assertEquals(0, new SurfaceStyle(0,0,0,-1,200,0).radius());

        SurfaceStyle style = new SurfaceStyle(0x80224433, 0x80224433, 0xAABBCCDD, 12, 16, 0x66000000);
        int normal = drawCalls(200, 200, style);
        int tall = drawCalls(200, 2000, style);
        assertTrue(normal < 2500, "single surface emitted " + normal + " primitive fills");
        assertTrue(tall <= normal + 16,
                "straight-edge height must not multiply shadow render states: " + normal + " -> " + tall);

        SurfaceStyle gradient = new SurfaceStyle(0x80224433, 0x80112211, 0xAABBCCDD, 12, 16, 0x66000000);
        int gradientNormal = drawCalls(200, 200, gradient);
        int gradientTall = drawCalls(200, 2000, gradient);
        assertTrue(gradientTall <= gradientNormal + 16,
                "gradient height must stay bounded too: " + gradientNormal + " -> " + gradientTall);
    }

    @Test void explicitRoundedRingDoesNotUnderpaintItsInterior() {
        int[][] hits = new int[24][24];
        RoundedGeometry.paintRing((x, y, w, h, color) -> {
            for (int xx = x; xx < x + w; xx++) for (int yy = y; yy < y + h; yy++) {
                assertEquals(1, ++hits[xx][yy], "Ring geometry overlapped itself");
            }
        }, 0, 0, 24, 24, 7, 3, 0x80FFFFFF);
        assertEquals(0, hits[12][12], "Border ring must not paint the interior");
        assertEquals(1, hits[0][7], "Expected left border pixel");
    }

    @Test void ordinaryRoundedPrimitivesNeverEmitTheSamePixelTwice() {
        for (int w = 1; w <= 24; w++) for (int h = 1; h <= 24; h++) {
            RecordingContext rect = new RecordingContext(w, h);
            rect.roundedRect(0, 0, w, h, 7, 0x80408040);

            RecordingContext outline = new RecordingContext(w, h);
            outline.roundedOutline(0, 0, w, h, 7, 0x80204020, 0x80FFFFFF);
        }
    }

    private static int drawCalls(int width, int height, SurfaceStyle style) {
        int[] calls = {0};
        SurfacePainter.paint((x, y, w, h, color) -> calls[0]++, 0, 0, width, height, style);
        return calls[0];
    }

    private static final class RecordingContext implements UiDrawContext {
        private final int width;
        private final int height;
        private final int[][] hits;

        private RecordingContext(int width, int height) {
            this.width = width;
            this.height = height;
            this.hits = new int[width][height];
        }

        @Override public int width() { return width; }
        @Override public int height() { return height; }

        @Override
        public void fill(int x, int y, int w, int h, int color) {
            assertTrue(w > 0 && h > 0, "fill must have positive bounds");
            assertTrue(x >= 0 && y >= 0 && x + w <= width && y + h <= height,
                    "fill escaped primitive bounds");
            for (int xx = x; xx < x + w; xx++) for (int yy = y; yy < y + h; yy++) {
                assertEquals(1, ++hits[xx][yy], "Alpha overdraw at " + xx + "," + yy);
            }
        }

        @Override public void surface(int x, int y, int width, int height, int radius, int fillColor, int borderColor, boolean elevated) { }
        @Override public void shadow(int x, int y, int width, int height, int radius) { }
        @Override public void text(Component text, int x, int y, int color, boolean shadow) { }
        @Override public void text(String text, int x, int y, int color, boolean shadow) { }
        @Override public void texture(UiTexture texture, int x, int y, int u, int v, int width, int height,
                                      int srcWidth, int srcHeight, int textureWidth, int textureHeight) { }
        @Override public void renderItem(ItemStack stack, int x, int y) { }
        @Override public void tooltip(Component text, int mouseX, int mouseY, int boundsX, int boundsY,
                                      int boundsWidth, int boundsHeight) { }
        @Override public void pushClip(int x, int y, int width, int height) { }
        @Override public void popClip() { }
        @Override public void pushTransform() { }
        @Override public void translate(float dx, float dy) { }
        @Override public void scale(float sx, float sy) { }
        @Override public void popTransform() { }
    }
}
