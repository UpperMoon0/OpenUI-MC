# Tinted gradient surfaces

`UiDrawContext.surface(x, y, width, height, SurfaceStyle)` paints a renderer-neutral tinted surface. The default implementation works on all adapters without adding abstract methods to existing drawing-context implementations.

```java
var glass = new SurfaceStyle(
        0xED304B3E, // top tint, straight ARGB
        0xF014241E, // bottom tint
        0xAA91B5A1, // border
        10,        // radius, clamped to the available bounds
        6,         // exterior shadow extent, clamped to 0..16
        0x68000000 // shadow
);
canvas.surface(x, y, width, height, glass);
```

The border and gradient occupy disjoint scanline spans, avoiding the opaque border underlay and repeated alpha blending of overlapping rectangles. Shadows use exterior rings. Allow padding for the shadow extent and its two-pixel vertical offset when cropping screenshots or clipping a surface.

This material gives a portable glass-like appearance through tint, transparency, gradients, and depth. It does not capture or blur a framebuffer. Foreground text, items, and tooltips remain in their usual rendering layers. Actual backdrop blur would require separate immediate and extraction renderer implementations plus resource lifecycle handling; no unsupported blur API is advertised.

Existing surface and rounded-rectangle overloads retain their behavior. `SurfacePainter` can also be used with a rectangle-fill callback for renderer-independent tests.
