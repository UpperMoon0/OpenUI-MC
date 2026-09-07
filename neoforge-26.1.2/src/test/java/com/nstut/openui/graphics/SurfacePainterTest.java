package com.nstut.openui.graphics;

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
    }
}
