package com.nstut.openui.api;

import com.nstut.openui.layout.Alignment;
import com.nstut.openui.layout.Constraints;
import com.nstut.openui.layout.Justification;
import com.nstut.openui.layout.Size;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

public class HStack extends UIComponent {

    private int gap = 0;
    private Alignment alignment = Alignment.STRETCH;
    private Justification justification = Justification.START;

    public HStack gap(int gap) { this.gap = Math.max(0, gap); invalidateLayout(); return this; }
    public HStack align(Alignment alignment) { this.alignment = alignment; invalidateLayout(); return this; }
    public HStack justify(Justification justification) { this.justification = justification; invalidateLayout(); return this; }
    public HStack child(UIComponent c) { addChild(c); return this; }

    private List<UIComponent> visibleChildren() {
        List<UIComponent> vc = new ArrayList<>();
        for (UIComponent c : children) if (c.isVisible()) vc.add(c);
        return vc;
    }

    @Override
    public int preferredWidth(Font font) {
        int total = 0;
        List<UIComponent> vc = visibleChildren();
        for (UIComponent c : vc) total += c.preferredWidth(font);
        if (!vc.isEmpty()) total += gap * (vc.size() - 1);
        return total;
    }

    @Override
    public int preferredHeight(Font font) {
        int max = 0;
        for (UIComponent c : visibleChildren()) max = Math.max(max, c.preferredHeight(font));
        return max;
    }

    @Override
    public void layout(int x, int y, int availableWidth, int availableHeight) {
        setBounds(x, y, availableWidth, availableHeight);
        List<UIComponent> vc = visibleChildren();
        if (vc.isEmpty()) return;
        float flexTotal = 0.0F;
        int fixedTotal = 0;
        for (UIComponent c : vc) {
            if (c instanceof Spacer || c.isFlex()) flexTotal += c.getFlexGrow() > 0.0F ? c.getFlexGrow() : 1.0F;
            else fixedTotal += c.measure(Constraints.loose(availableWidth, availableHeight), measureFont()).width();
        }
        int baseGaps = gap * Math.max(0, vc.size() - 1);
        int flexSpace = Math.max(0, availableWidth - fixedTotal - baseGaps);

        List<Integer> widths = new ArrayList<>();
        int distributed = 0;
        float usedWeight = 0.0F;
        for (UIComponent c : vc) {
            if ((c instanceof Spacer || c.isFlex()) && flexTotal > 0.0F) {
                usedWeight += c.getFlexGrow() > 0.0F ? c.getFlexGrow() : 1.0F;
                int target = Math.round(flexSpace * usedWeight / flexTotal);
                widths.add(target - distributed);
                distributed = target;
            } else {
                widths.add(c.measure(Constraints.loose(availableWidth, availableHeight), measureFont()).width());
            }
        }

        int contentWidth = widths.stream().mapToInt(Integer::intValue).sum() + baseGaps;
        int extra = Math.max(0, availableWidth - contentWidth);
        int startOffset = 0;
        int extraGap = 0;
        switch (justification) {
            case CENTER -> startOffset = extra / 2;
            case END -> startOffset = extra;
            case SPACE_BETWEEN -> extraGap = vc.size() > 1 ? extra / (vc.size() - 1) : 0;
            case SPACE_AROUND -> { extraGap = extra / vc.size(); startOffset = extraGap / 2; }
            case SPACE_EVENLY -> { extraGap = extra / (vc.size() + 1); startOffset = extraGap; }
            default -> { }
        }

        int cx = x + startOffset;
        for (int i = 0; i < vc.size(); i++) {
            UIComponent c = vc.get(i);
            int cw = widths.get(i);
            Size measured = c.measure(Constraints.loose(cw, availableHeight), measureFont());
            int ch = alignment == Alignment.STRETCH ? availableHeight : measured.height();
            int childY = switch (alignment) {
                case CENTER -> y + (availableHeight - ch) / 2;
                case END -> y + availableHeight - ch;
                default -> y;
            };
            c.layout(cx, childY, cw, ch);
            cx += cw + gap + extraGap;
        }
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        for (UIComponent c : children) if (c.isVisible()) c.render(g, font, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent c = children.get(i);
            if (c.isVisible() && c.mouseClicked(mx, my, button)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for (UIComponent c : children) if (c.isVisible() && c.mouseScrolled(mx, my, delta)) return true;
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        for (UIComponent c : children) if (c.isVisible() && c.mouseDragged(mx, my, button, dragX, dragY)) return true;
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for (UIComponent c : children) if (c.isVisible() && c.mouseReleased(mx, my, button)) return true;
        return false;
    }
}
