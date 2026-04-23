package com.xioyim.titlepro.client;

import com.xioyim.titlepro.scheme.TitleProScheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * A simple scrollable list screen that shows all saved TitlePro scheme files.
 * Clicking a name calls the {@code onSelect} callback with the loaded SchemeData.
 */
public class SchemeListScreen extends Screen {

    private static final int ITEM_H = 22;
    private static final int M      = 10;

    private final Screen parent;
    private final Consumer<TitleProScheme.SchemeData> onSelect;

    private List<String> names;
    private int  scrollOffset = 0;
    private String statusMsg  = "";

    public SchemeListScreen(Screen parent, Consumer<TitleProScheme.SchemeData> onSelect) {
        super(Component.translatable("screen.titlepro.scheme_list"));
        this.parent   = parent;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        names = TitleProScheme.listNames();

        // Back button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.back"),
                b -> this.minecraft.setScreen(parent))
            .bounds(M, this.height - 26, 70, 18).build());

        // Refresh button
        this.addRenderableWidget(Button.builder(
                Component.translatable("titlepro.gui.scheme_list.refresh"),
                b -> { names = TitleProScheme.listNames(); scrollOffset = 0; statusMsg = ""; })
            .bounds(M + 74, this.height - 26, 60, 18).build());
    }

    // ── list area bounds ──────────────────────────────────────────────────────

    private int listTop()    { return 24; }
    private int listBottom() { return this.height - 32; }
    private int listH()      { return listBottom() - listTop(); }

    // ── render ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        int lt = listTop(), lb = listBottom(), lw = this.width - 2 * M;
        g.fill(M, lt, M + lw, lb, 0x88000000);

        if (names.isEmpty()) {
            g.drawCenteredString(this.font,
                    Component.translatable("titlepro.gui.scheme_list.empty"),
                    this.width / 2, lt + listH() / 2 - 4, 0xAAAAAA);
        } else {
            for (int i = 0; i < names.size(); i++) {
                int itemY = lt + 4 + i * ITEM_H - scrollOffset;
                // skip items completely outside the list area
                if (itemY + ITEM_H <= lt || itemY >= lb) continue;

                boolean hovered = mouseX >= M + 2 && mouseX < M + lw - 2
                               && mouseY >= itemY  && mouseY < itemY + ITEM_H;
                if (hovered) {
                    int top = Math.max(itemY, lt);
                    int bot = Math.min(itemY + ITEM_H - 1, lb - 1);
                    g.fill(M + 2, top, M + lw - 2, bot, 0x55FFFFFF);
                }
                int textY = itemY + (ITEM_H - this.font.lineHeight) / 2;
                if (textY >= lt && textY + this.font.lineHeight <= lb) {
                    g.drawString(this.font, names.get(i), M + 8, textY, 0xFFFFFF, false);
                }
            }
        }

        // Hint text — drawn just inside the bottom of the list area so buttons don't cover it
        g.drawString(this.font,
                Component.translatable("titlepro.gui.scheme_list.click_hint"),
                M + 4, lb - this.font.lineHeight - 3, 0x888888, false);

        super.render(g, mouseX, mouseY, partial);

        // Error / status overlay
        if (!statusMsg.isEmpty()) {
            g.drawCenteredString(this.font, statusMsg, this.width / 2, this.height - 10, 0xFFFF55);
        }
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        int lt = listTop(), lb = listBottom(), lw = this.width - 2 * M;
        if (mouseX < M + 2 || mouseX >= M + lw - 2 || mouseY < lt || mouseY >= lb) return false;

        for (int i = 0; i < names.size(); i++) {
            int itemY = lt + 4 + i * ITEM_H - scrollOffset;
            if (mouseY >= itemY && mouseY < itemY + ITEM_H) {
                try {
                    TitleProScheme.SchemeData sd = TitleProScheme.load(names.get(i));
                    onSelect.accept(sd);
                } catch (Exception e) {
                    statusMsg = "§c" + Component.translatable("titlepro.gui.status.scheme_load_error").getString()
                                + ": " + e.getMessage();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, names.size() * ITEM_H - listH() + 8);
        scrollOffset  = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(delta * ITEM_H)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
