package com.nstut.openui.controls;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.overlay.OverlayManager;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContextMenu extends UIComponent {
    public sealed interface Entry permits Item, Separator {}

    public record Item(Component label, IconWidget icon, Runnable action, boolean enabled, String shortcut) implements Entry {
        public Item(String label, Runnable action) {
            this(Component.literal(label), null, action, true, null);
        }
        public Item(Component label, Runnable action) {
            this(label, null, action, true, null);
        }
        public Item(String label, IconWidget icon, Runnable action) {
            this(Component.literal(label), icon, action, true, null);
        }
    }

    public record Separator() implements Entry {}

    private final List<Entry> entries = new ArrayList<>();
    private final int mouseX, mouseY;
    private OverlayHandle handle;

    public ContextMenu(int mouseX, int mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        focusable(true);
    }

    public ContextMenu item(String label, Runnable action) {
        entries.add(new Item(label, action));
        return this;
    }

    public ContextMenu item(Component label, Runnable action) {
        entries.add(new Item(label, action));
        return this;
    }

    public ContextMenu item(String label, IconWidget icon, Runnable action) {
        entries.add(new Item(label, icon, action));
        return this;
    }

    public ContextMenu separator() {
        entries.add(new Separator());
        return this;
    }

    public OverlayHandle show(OverlayManager overlays) {
        if (overlays == null) return null;
        handle = overlays.show(
                OverlayLayer.POPOVER,
                this,
                false,
                true,
                true,
                () -> handle = null
        );
        return handle;
    }

    public void close() {
        if (handle != null) {
            handle.close();
            handle = null;
        }
    }

    @Override
    public int preferredWidth(Font font) {
        if (font == null) return 100;
        int maxW = 80;
        for (Entry entry : entries) {
            if (entry instanceof Item item) {
                int w = font.width(item.label());
                if (item.shortcut() != null) w += font.width(item.shortcut()) + 16;
                if (item.icon() != null) w += 18;
                maxW = Math.max(maxW, w);
            }
        }
        return maxW + 20;
    }

    @Override
    public int preferredHeight(Font font) {
        int h = 6;
        for (Entry entry : entries) {
            h += (entry instanceof Separator) ? 5 : 16;
        }
        return h;
    }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        Font font = measureFont();
        int menuW = preferredWidth(font);
        int menuH = preferredHeight(font);

        int minX = lx + 2;
        int maxX = lx + Math.max(0, availableWidth - menuW - 2);
        int minY = ly + 2;
        int maxY = ly + Math.max(0, availableHeight - menuH - 2);

        int targetX = mouseX;
        int targetY = mouseY;

        if (targetX + menuW > lx + availableWidth) targetX = Math.max(minX, mouseX - menuW);
        if (targetY + menuH > ly + availableHeight) targetY = Math.max(minY, mouseY - menuH);

        targetX = Math.max(minX, Math.min(maxX, targetX));
        targetY = Math.max(minY, Math.min(maxY, targetY));

        setBounds(targetX, targetY, menuW, menuH);
    }

    @Override
    public void render(GuiGraphicsExtractor g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.cardTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.cardTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

        int curY = y + 3;
        for (Entry entry : entries) {
            if (entry instanceof Separator) {
                g.fill(x + 4, curY + 2, x + width - 4, curY + 3, colors.borderSubtle());
                curY += 5;
            } else if (entry instanceof Item item) {
                int itemH = 16;
                boolean isItemHovered = item.enabled() && mx >= x + 2 && mx < x + width - 2 && my >= curY && my < curY + itemH;

                if (isItemHovered) {
                    UiRender.roundedRect(g, x + 3, curY, width - 6, itemH, 2, colors.surfaceVariant());
                }

                int textX = x + 8;
                if (item.icon() != null) {
                    item.icon().layout(textX, curY + 2, 12, 12);
                    item.icon().render(g, font, mx, my, pt);
                    textX += 16;
                }

                int textCol = item.enabled() ? (isItemHovered ? colors.primaryHover() : colors.onSurface()) : colors.onSurfaceDisabled();
                g.text(font, item.label(), textX, curY + (itemH - font.lineHeight) / 2, textCol);

                if (item.shortcut() != null) {
                    int scW = font.width(item.shortcut());
                    g.text(font, item.shortcut(), x + width - scW - 8, curY + (itemH - font.lineHeight) / 2, colors.onSurfaceMuted());
                }
                curY += itemH;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= x && mx < x + width) {
            int curY = y + 3;
            for (Entry entry : entries) {
                if (entry instanceof Separator) {
                    curY += 5;
                } else if (entry instanceof Item item) {
                    int itemH = 16;
                    if (my >= curY && my < curY + itemH && item.enabled()) {
                        close();
                        if (item.action() != null) item.action().run();
                        return true;
                    }
                    curY += itemH;
                }
            }
        }
        return false;
    }
}
