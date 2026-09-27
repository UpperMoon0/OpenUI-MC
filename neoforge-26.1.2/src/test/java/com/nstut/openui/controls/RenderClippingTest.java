package com.nstut.openui.controls;

import com.nstut.openui.api.Panel;
import com.nstut.openui.api.StyledBox;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.style.StateStyle;
import com.nstut.openui.style.Style;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RenderClippingTest {
    private static final int MARKER_A = 0x13579BDF;
    private static final int MARKER_B = 0x2468ACE0;

    private record Rect(int minX, int minY, int maxX, int maxY) { }
    private record Paint(int color, Rect bounds, Rect clip) { }

    private static final class PaintBox extends UIComponent {
        private final int markerColor;
        private PaintBox(int markerColor) { this.markerColor = markerColor; }
        @Override public int preferredWidth(Font font) { return 200; }
        @Override public int preferredHeight(Font font) { return 300; }
        @Override public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
            g.fill(x - 100, y - 100, x + width + 100, y + height + 100, markerColor);
        }
    }

    private static final class RecordingGraphics extends GuiGraphicsExtractor {
        private List<Rect> enabledScissors;
        private Deque<Rect> activeScissors;
        private List<Paint> paints;
        private int disableCalls;

        private RecordingGraphics() { super(null, null, 0, 0); }

        static RecordingGraphics create() {
            RecordingGraphics graphics = allocateWithoutConstructor(RecordingGraphics.class);
            graphics.enabledScissors = new ArrayList<>();
            graphics.activeScissors = new ArrayDeque<>();
            graphics.paints = new ArrayList<>();
            return graphics;
        }

        @Override public void enableScissor(int minX, int minY, int maxX, int maxY) {
            Rect rect = new Rect(minX, minY, maxX, maxY);
            enabledScissors.add(rect);
            activeScissors.push(rect);
        }

        @Override public void disableScissor() {
            disableCalls++;
            if (activeScissors.isEmpty()) throw new AssertionError("scissor pop without matching push");
            activeScissors.pop();
        }

        @Override public void fill(int minX, int minY, int maxX, int maxY, int color) {
            paints.add(new Paint(color, new Rect(minX,minY,maxX,maxY), currentClip()));
        }

        Rect currentClip() { return activeScissors.peek(); }
        Paint paint(int color) {
            return paints.stream().filter(paint -> paint.color() == color).findFirst().orElseThrow();
        }
        boolean colorCovers(int color,int px,int py) {
            return paints.stream().filter(paint -> paint.color()==color).anyMatch(paint -> {
                Rect b=paint.bounds();
                return px>=b.minX()&&px<b.maxX()&&py>=b.minY()&&py<b.maxY();
            });
        }
        boolean effectivelyCovers(int color,int px,int py) {
            return paints.stream().filter(paint -> paint.color()==color).anyMatch(paint -> {
                Rect b=paint.bounds(), c=paint.clip();
                boolean inBounds=px>=b.minX()&&px<b.maxX()&&py>=b.minY()&&py<b.maxY();
                boolean inClip=c==null || (px>=c.minX()&&px<c.maxX()&&py>=c.minY()&&py<c.maxY());
                return inBounds&&inClip;
            });
        }
    }

    private static Font font() { return new Font(null); }

    @SuppressWarnings("unchecked")
    private static <T> T allocateWithoutConstructor(Class<T> type) {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (T) ((Unsafe) field.get(null)).allocateInstance(type);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to allocate recording GuiGraphicsExtractor", e);
        }
    }

    @Test
    void cardRenderClipsOversizedChildToContentInteriorAndLeavesSurfaceOutsideOwnClip() {
        PaintBox child = new PaintBox(MARKER_A);
        Card card = new Card(child).padding(4).elevated(true);
        card.layoutTree(font(), 10, 20, 100, 60);
        RecordingGraphics graphics = RecordingGraphics.create();

        card.render(graphics, font(), 0, 0, 0);

        Rect contentClip = new Rect(14, 24, 106, 76);
        assertEquals(List.of(contentClip), graphics.enabledScissors);
        assertEquals(contentClip, graphics.paint(MARKER_A).clip(),
                "oversized descendant paint must execute under the card content scissor");
        assertTrue(graphics.paints.stream()
                        .filter(paint -> paint.color() != MARKER_A)
                        .allMatch(paint -> paint.clip() == null),
                "card shadow, background, and border must render before the card's own child clip");
        assertEquals(1, graphics.disableCalls);
        assertNull(graphics.currentClip(), "card render must restore the incoming scissor state");
    }

    @Test
    void panelAndStyledBoxRenderUseTheirContentInteriors() {
        Panel panel = new Panel().padding(6).child(new PaintBox(MARKER_A));
        panel.layoutTree(font(), 10, 20, 100, 60);
        RecordingGraphics panelGraphics = RecordingGraphics.create();
        panel.render(panelGraphics, font(), 0, 0, 0);
        assertEquals(List.of(new Rect(16, 26, 104, 74)), panelGraphics.enabledScissors);
        assertEquals(new Rect(16, 26, 104, 74), panelGraphics.paint(MARKER_A).clip());
        assertNull(panelGraphics.currentClip());

        Style style = Style.builder()
                .margin(3)
                .padding(4)
                .background(0xFF111111)
                .border(2, 0xFFEEEEEE)
                .build();
        StyledBox styled = Ui.styled(StateStyle.of(style), new PaintBox(MARKER_B));
        styled.layoutTree(font(), 10, 20, 100, 60);
        RecordingGraphics styledGraphics = RecordingGraphics.create();
        styled.render(styledGraphics, font(), 0, 0, 0);
        assertEquals(List.of(new Rect(17, 27, 103, 73)), styledGraphics.enabledScissors);
        assertEquals(new Rect(17, 27, 103, 73), styledGraphics.paint(MARKER_B).clip());
        assertNull(styledGraphics.currentClip());
    }

    @Test
    void lowPaddingRoundedSurfaceUsesRadiusSafeContentRectangle() {
        PaintBox cardChild=new PaintBox(MARKER_B);
        Card card=new Card(cardChild).padding(0).radius(6).elevated(false);
        card.layoutTree(font(),10,20,100,60);
        RecordingGraphics cardGraphics=RecordingGraphics.create();
        card.render(cardGraphics,font(),0,0,0);
        Rect contentClip=new Rect(13,23,107,77);
        assertEquals(List.of(contentClip),cardGraphics.enabledScissors,
                "zero-padding rounded Card must reserve a radius-safe content rectangle");
        assertEquals(13,cardChild.getX());
        assertEquals(94,cardChild.getWidth());
        assertEquals(contentClip,cardGraphics.paint(MARKER_B).clip());
        assertTrue(cardGraphics.effectivelyCovers(MARKER_B,13,23));
        assertFalse(cardGraphics.effectivelyCovers(MARKER_B,12,22),
                "rounded Card corner/border arc must remain outside effective child paint");

        int background=0xFF112233, border=0xFFCCDDEE;
        PaintBox child=new PaintBox(MARKER_A);
        Panel panel=new Panel(background,border).padding(0).radius(6).child(child);
        panel.layoutTree(font(),10,20,100,60);
        RecordingGraphics graphics=RecordingGraphics.create();
        panel.render(graphics,font(),0,0,0);

        assertEquals(List.of(contentClip),graphics.enabledScissors);
        assertEquals(13,child.getX());
        assertEquals(23,child.getY());
        assertEquals(94,child.getWidth());
        assertEquals(54,child.getHeight());
        assertEquals(contentClip,graphics.paint(MARKER_A).clip());
        assertTrue(graphics.colorCovers(background,contentClip.minX(),contentClip.minY()),
                "top-left child clip corner must lie inside the rounded inner fill");
        assertTrue(graphics.colorCovers(background,contentClip.maxX()-1,contentClip.minY()));
        assertTrue(graphics.colorCovers(background,contentClip.minX(),contentClip.maxY()-1));
        assertTrue(graphics.colorCovers(background,contentClip.maxX()-1,contentClip.maxY()-1));
        assertFalse(graphics.colorCovers(background,12,22),
                "pixel just outside the radius-safe rectangle remains rounded border/corner territory");
    }

    @Test
    void styledBorderWidthAndRoundedContentRegionAgree() {
        int background=0xFF223344, border=0xFFABCDEF;
        Style style=Style.builder().padding(0).background(background).border(4,border).radius(8).build();
        PaintBox child=new PaintBox(MARKER_B);
        StyledBox styled=Ui.styled(StateStyle.of(style),child);
        styled.layoutTree(font(),10,20,100,60);
        RecordingGraphics graphics=RecordingGraphics.create();
        styled.render(graphics,font(),0,0,0);

        Rect contentClip=new Rect(15,25,105,75);
        assertEquals(List.of(contentClip),graphics.enabledScissors);
        assertEquals(15,child.getX());
        assertEquals(90,child.getWidth());
        assertEquals(contentClip,graphics.paint(MARKER_B).clip());
        assertTrue(graphics.effectivelyCovers(MARKER_B,15,25));
        assertFalse(graphics.effectivelyCovers(MARKER_B,14,24),
                "rounded StyledBox border arc must remain outside effective child paint");
        assertTrue(graphics.colorCovers(background,15,25));
        assertFalse(graphics.colorCovers(background,14,24),
                "styled content must not enter the rounded multi-pixel border arc");
        assertTrue(graphics.colorCovers(border,14,24),
                "configured StyledBox border width must actually be rendered");
    }

    @Test
    void nestedRenderIntersectsInnerClipAndRestoresOuterClipForLaterSiblings() {
        PaintBox innerPaint = new PaintBox(MARKER_A);
        Card inner = new Card(innerPaint).padding(5).elevated(false);
        PaintBox sibling = new PaintBox(MARKER_B);
        Card outer = new Card().padding(5).elevated(false);
        outer.addChild(inner);
        outer.addChild(sibling);
        outer.layoutTree(font(), 0, 0, 100, 80);

        // Deliberately place the inner surface partially outside the outer content box.
        // The second scissor must be the intersection, not the raw inner rectangle.
        inner.layoutTree(font(), 70, 10, 50, 50);
        RecordingGraphics graphics = RecordingGraphics.create();

        outer.render(graphics, font(), 0, 0, 0);

        Rect outerClip = new Rect(5, 5, 95, 75);
        Rect intersectedInnerClip = new Rect(75, 15, 95, 55);
        assertEquals(List.of(outerClip, intersectedInnerClip), graphics.enabledScissors);
        assertEquals(intersectedInnerClip, graphics.paint(MARKER_A).clip(),
                "nested child paint must use ClipStack's intersected scissor");
        assertEquals(outerClip, graphics.paint(MARKER_B).clip(),
                "popping the nested clip must restore the outer scissor for the next sibling");
        assertEquals(2, graphics.disableCalls);
        assertNull(graphics.currentClip(), "all nested scissor state must be restored after render");
    }
}
