package com.nstut.openui.controls;

import com.nstut.openui.api.ClipStack;
import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.api.UiRender;
import com.nstut.openui.overlay.OverlayHandle;
import com.nstut.openui.overlay.OverlayLayer;
import com.nstut.openui.overlay.OverlayManager;
import com.nstut.openui.theme.ColorScheme;
import com.nstut.openui.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandPalette extends UIComponent {
    public record CommandItem(String id, Component title, Component subtitle, String shortcutHint, Runnable action) {
        public CommandItem(String id, String title, String subtitle, Runnable action) {
            this(id, Component.literal(title), subtitle != null ? Component.literal(subtitle) : null, null, action);
        }
        public CommandItem(String id, String title, Runnable action) {
            this(id, Component.literal(title), null, null, action);
        }
    }

    private final List<CommandItem> items = new ArrayList<>();
    private final List<CommandItem> filteredItems = new ArrayList<>();
    private String query = "";
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private OverlayHandle handle;

    public CommandPalette() {
        focusable(true);
    }

    public static OverlayHandle show(OverlayManager overlays, List<CommandItem> items) {
        if (overlays == null) return null;
        CommandPalette palette = new CommandPalette();
        if (items != null) palette.items.addAll(items);
        palette.filter();

        Dialog.DialogContainer container = new Dialog.DialogContainer(palette);
        OverlayHandle handle = overlays.show(
                OverlayLayer.MODAL,
                container,
                true,
                true,
                true,
                null
        );
        palette.handle = handle;
        return handle;
    }

    public CommandPalette item(String id, String title, Runnable action) {
        items.add(new CommandItem(id, title, action));
        filter();
        return this;
    }

    public CommandPalette item(String id, String title, String subtitle, Runnable action) {
        items.add(new CommandItem(id, title, subtitle, action));
        filter();
        return this;
    }

    public CommandPalette item(CommandItem item) {
        items.add(Objects.requireNonNull(item));
        filter();
        return this;
    }

    private void filter() {
        filteredItems.clear();
        String q = query.trim().toLowerCase();
        for (CommandItem item : items) {
            if (q.isEmpty() || item.title().getString().toLowerCase().contains(q)
                    || (item.subtitle() != null && item.subtitle().getString().toLowerCase().contains(q))) {
                filteredItems.add(item);
            }
        }
        selectedIndex = Math.max(0, Math.min(filteredItems.size() - 1, selectedIndex));
        invalidatePaint();
    }

    @Override
    public int preferredWidth(Font font) { return 280; }

    @Override
    public int preferredHeight(Font font) { return 180; }

    @Override
    public void layout(int lx, int ly, int availableWidth, int availableHeight) {
        int w = preferredWidth(measureFont());
        int h = preferredHeight(measureFont());
        setBounds(lx + (availableWidth - w) / 2, ly + (availableHeight - h) / 3, w, h);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        Theme t = theme();
        ColorScheme colors = t.colors();

        UiRender.shadow(g, x, y, width, height, t.cardTheme().radius());
        UiRender.roundedOutline(g, x, y, width, height, t.cardTheme().radius(), colors.surfaceRaised(), colors.borderStrong());

        int textH = font != null ? font.lineHeight : 9;

        // Search bar header
        UiRender.roundedRect(g, x + 6, y + 6, width - 12, 22, 3, colors.input());
        if (font != null) {
            String prompt = query.isEmpty() ? "Type a command or search..." : query;
            int promptCol = query.isEmpty() ? colors.onSurfaceMuted() : colors.onSurface();
            UiRender.text(g, font, prompt, x + 12, y + 6 + (22 - textH) / 2, promptCol);
            if (!query.isEmpty()) {
                int cursorX = x + 12 + font.width(query);
                if ((System.currentTimeMillis() / 500) % 2 == 0) {
                    g.fill(cursorX, y + 9, cursorX + 1, y + 23, colors.primary());
                }
            }
        }

        // Separator
        g.fill(x + 6, y + 32, x + width - 6, y + 33, colors.borderSubtle());

        // Results list
        int listY = y + 36;
        int listH = height - 42;
        int itemH = 20;
        int maxVisible = Math.max(1, listH / itemH);
        int maxScroll = Math.max(0, filteredItems.size() - maxVisible);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        if (filteredItems.isEmpty()) {
            if (font != null) {
                UiRender.text(g, font, "No matching commands", x + (width - font.width("No matching commands")) / 2, listY + 20, colors.onSurfaceMuted());
            }
            return;
        }

        ClipStack.push(g, x + 6, listY, width - 12, listH);
        try {
            int start = scrollOffset;
            int end = Math.min(filteredItems.size(), start + maxVisible + 1);

            for (int i = start; i < end; i++) {
                CommandItem item = filteredItems.get(i);
                int rowY = listY + (i - start) * itemH;
                if (rowY + itemH > listY + listH) break;

                boolean isSelected = (i == selectedIndex);
                boolean isHovered = (mx >= x + 6 && mx < x + width - 6 && my >= rowY && my < rowY + itemH);

                if (isSelected || isHovered) {
                    int rowBg = isSelected ? colors.primaryDim() : colors.surfaceVariant();
                    UiRender.roundedRect(g, x + 6, rowY, width - 12, itemH, 2, rowBg);
                }

                if (font != null) {
                    int titleCol = isSelected ? colors.primaryHover() : colors.onSurface();
                    UiRender.text(g, font, item.title(), x + 12, rowY + (itemH - textH) / 2, titleCol);

                    if (item.subtitle() != null) {
                        int subX = x + 16 + font.width(item.title());
                        UiRender.text(g, font, item.subtitle(), subX, rowY + (itemH - textH) / 2, colors.onSurfaceMuted());
                    }

                    if (item.shortcutHint() != null) {
                        int hintW = font.width(item.shortcutHint());
                        UiRender.text(g, font, item.shortcutHint(), x + width - hintW - 14, rowY + (itemH - textH) / 2, colors.onSurfaceMuted());
                    }
                }
            }
        } finally {
            ClipStack.pop(g);
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 264) { // Down
            if (selectedIndex + 1 < filteredItems.size()) {
                selectedIndex++;
                ensureVisible();
                invalidatePaint();
            }
            return true;
        }
        if (key == 265) { // Up
            if (selectedIndex - 1 >= 0) {
                selectedIndex--;
                ensureVisible();
                invalidatePaint();
            }
            return true;
        }
        if (key == 257) { // Enter
            executeSelected();
            return true;
        }
        if (key == 259) { // Backspace
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                filter();
            }
            return true;
        }
        if (key == 256) { // Escape
            if (handle != null) handle.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta != 0) {
            int listH = height - 42;
            int itemH = 20;
            int maxVisible = Math.max(1, listH / itemH);
            int maxScroll = Math.max(0, filteredItems.size() - maxVisible);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(delta)));
            invalidatePaint();
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (character >= 32 && character != 127) {
            query += character;
            filter();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && mx >= x + 6 && mx < x + width - 6) {
            int listY = y + 36;
            int itemH = 20;
            int listH = height - 42;
            int maxVisible = Math.max(1, listH / itemH);
            int visiblePixelHeight = maxVisible * itemH;
            int relativeY = (int) my - listY;
            if (relativeY < 0 || relativeY >= visiblePixelHeight) return false;
            int clickedIdx = scrollOffset + (int) ((my - listY) / itemH);
            if (clickedIdx >= 0 && clickedIdx < filteredItems.size()) {
                selectedIndex = clickedIdx;
                executeSelected();
                return true;
            }
        }
        return false;
    }

    private void ensureVisible() {
        int listH = height - 42;
        int itemH = 20;
        int maxVisible = Math.max(1, listH / itemH);
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + maxVisible) {
            scrollOffset = selectedIndex - maxVisible + 1;
        }
    }

    private void executeSelected() {
        if (selectedIndex >= 0 && selectedIndex < filteredItems.size()) {
            CommandItem item = filteredItems.get(selectedIndex);
            if (handle != null) handle.close();
            if (item.action() != null) item.action().run();
        }
    }
}
