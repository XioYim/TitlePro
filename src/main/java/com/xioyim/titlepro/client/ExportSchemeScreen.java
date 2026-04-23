package com.xioyim.titlepro.client;

import com.xioyim.titlepro.data.TitleData;
import com.xioyim.titlepro.scheme.TitleProScheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dialog screen for naming and saving a TitlePro scheme to disk.
 * Returns to the parent screen (TitleProScreen) after export.
 */
public class ExportSchemeScreen extends Screen {

    private static final int EP_W = 300;
    private static final int EP_H = 56;

    private final Screen   parent;
    private final TitleData data;
    private final boolean  extraCmdEnabled;
    private final String   extraCmd;

    private EditBox fieldName;
    private String  statusMsg = "";

    public ExportSchemeScreen(Screen parent, TitleData data,
                              boolean extraCmdEnabled, String extraCmd) {
        super(Component.translatable("screen.titlepro.export_scheme"));
        this.parent          = parent;
        this.data            = data;
        this.extraCmdEnabled = extraCmdEnabled;
        this.extraCmd        = extraCmd != null ? extraCmd : "";
    }

    @Override
    protected void init() {
        int cx  = this.width  / 2;
        int cy  = this.height / 2;
        int epY = cy - EP_H / 2;

        // Layout inside panel: [name field] [导出 btn] [取消 btn]  — all on one row
        // EP_W=300: field=180, 导出=44, 取消=44, gaps+margins account for the rest
        int fieldX = cx - EP_W / 2 + 8;   // left inner edge of panel
        int fieldW = 180;
        int btnY   = epY + 24;

        fieldName = new EditBox(this.font, fieldX, btnY, fieldW, 18, Component.empty());
        fieldName.setMaxLength(80);
        fieldName.setHint(Component.translatable("titlepro.gui.export.name_hint"));
        this.addRenderableWidget(fieldName);

        // 导出 confirm button — immediately right of field
        this.addRenderableWidget(Button.builder(
                Component.translatable("titlepro.gui.export.confirm"),
                b -> doExport())
            .bounds(fieldX + fieldW + 4, btnY, 44, 18).build());

        // 取消 cancel button — right of 导出
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                b -> this.minecraft.setScreen(parent))
            .bounds(fieldX + fieldW + 4 + 44 + 4, btnY, 44, 18).build());
    }

    private void doExport() {
        String name = fieldName.getValue().trim();
        if (name.isBlank()) {
            statusMsg = Component.translatable("titlepro.gui.status.export_no_name").getString();
            return;
        }
        try {
            TitleProScheme.save(name, data, extraCmdEnabled, extraCmd);
            // Pass success status to parent TitleProScreen before switching back
            if (parent instanceof TitleProScreen tps) {
                tps.setStatus(Component.translatable("titlepro.gui.status.exported").getString());
            }
            this.minecraft.setScreen(parent);
        } catch (Exception e) {
            statusMsg = "§c导出失败: " + e.getMessage();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ENTER or NUMPAD_ENTER confirms the export
        if (keyCode == 257 || keyCode == 335) {
            doExport();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);

        int cx  = this.width  / 2;
        int cy  = this.height / 2;
        int epX = cx - EP_W / 2;
        int epY = cy - EP_H / 2;

        // Panel border + fill — drawn BEFORE widgets so widgets sit on top
        g.fill(epX - 1, epY - 1, epX + EP_W + 1, epY + EP_H + 1, 0xFF555555);
        g.fill(epX,     epY,     epX + EP_W,     epY + EP_H,     0xFF1E1E1E);

        // Panel title
        g.drawCenteredString(this.font,
                Component.translatable("titlepro.gui.export.title"),
                cx, epY + 8, 0xFFFFFF);

        super.render(g, mx, my, pt);

        // Status / error message
        if (!statusMsg.isEmpty()) {
            g.drawCenteredString(this.font, statusMsg, cx, epY + EP_H + 8, 0xFFFF55);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
