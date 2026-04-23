package com.xioyim.titlepro.client;

import com.xioyim.titlepro.command.TitleProParser;
import com.xioyim.titlepro.data.TitleData;
import com.xioyim.titlepro.scheme.TitleProScheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TitlePro editor screen.
 *
 * Layout (rows computed from dynamic text-box height):
 *   Row 0 (y0): multi-line text input — auto-expands with content
 *   Row 1 (y1): X / Y / scale                    | [从配置文件中导入]
 *   Row 2 (y2): stay / fadeIn / fadeOut           | [从剪贴板导入]
 *   Row 3 (y3): [exitSlide btn] | exitSpeed | spacing | [预览标题]
 *   Row 4 (y4): [bgType btn] | bgColor | bgAlpha  | [发送给自己]
 *   Row 5 (y5): bgPadX/shadowX | bgPadY/shadowY | bgOffsetY(bg) | [生成并复制]
 *   Row 6 (y6): [额外命令 toggle] | [extra cmd input full-width] | [重置内容]
 *   Row 7 (y7): right column only → [叠加方向]
 *   Row 8 (y8): right column only → [对齐方式]
 *   Row 9 (y9): right column only → [导出方案]
 */
public class TitleProScreen extends Screen {

    // ── Fixed layout constants ─────────────────────────────────────────────
    private static final int FH          = 18;  // field / button height
    private static final int M           = 6;   // side margin
    private static final int ROW_GAP     = 6;   // vertical gap between rows
    private static final int ROW_STEP    = FH + ROW_GAP;
    private static final int BTN_COL     = 86;  // right button column width
    private static final int BTN_GAP     = 4;   // gap between form and button column
    private static final int TITLE_H     = 14;  // space for screen title
    private static final int EXTRA_Y     = 6;   // extra gap below title
    private static final int MAX_FIELD_W = 55;
    private static final int MIN_TEXT_H  = 32;  // minimum text box height
    private static final int MAX_TEXT_H  = 144; // maximum text box height

    // ── Dynamic row Y positions (instance vars, computed each init) ────────
    private int y0, y1, y2, y3, y4, y5, y6, y7, y8, y9;
    private int ySep, yPrev;
    /** Current text-box height in pixels — updated by tick() for auto-expand. */
    private int textBoxH = MIN_TEXT_H;
    /**
     * Text value to restore on next init() call.
     * null = read from CACHE (normal open / import).
     */
    private String pendingText = null;

    // ── Session cache (persists while the game is running) ─────────────────
    private static final FieldCache CACHE = new FieldCache();

    private static class FieldCache {
        String text       = "";
        String x = "0", y = "0", scale = "1.0";
        String stay = "5.0", fadeIn = "0.0", fadeOut = "1.0";
        String spacing    = "14";
        String bgColor    = "#000000", bgAlpha = "50";
        String bgPadX     = "3", bgPadY = "1", bgOffsetY = "-0.3";
        String shadowOffX = "0.4", shadowOffY = "0.4";
        String exitSpeed  = "1.0";
        boolean exitSlide     = true;
        boolean stackUp       = false;
        /** 0 = shadow, 1 = background */
        int bgType = 1;
        /** 0 = left, 1 = center, 2 = right */
        int textAlign = 1;
        boolean extraCmdEnabled = false;
        String  extraCmd        = "";
    }

    // ── Widgets ───────────────────────────────────────────────────────────
    private MultiLineEditBox textBox;
    private EditBox fieldX, fieldY, fieldScale;
    private EditBox fieldStay, fieldFadeIn, fieldFadeOut;
    private EditBox fieldSpacing;
    private EditBox fieldBgColor, fieldBgAlpha;
    private EditBox fieldExitSpeed;
    /** Background mode only (null in shadow mode) */
    private EditBox fieldBgPadX, fieldBgPadY, fieldBgOffsetY;
    /** Shadow mode only (null in background mode) */
    private EditBox fieldShadowX, fieldShadowY;
    private Button btnExitSlide, btnBgType, btnStack, btnAlign;
    /** Extra command row */
    private EditBox fieldExtraCmd;
    private Button  btnExtraCmd;

    // ── State ──────────────────────────────────────────────────────────────
    private boolean exitSlide       = CACHE.exitSlide;
    private boolean stackUp         = CACHE.stackUp;
    /** 0 = shadow, 1 = background */
    private int     bgType          = CACHE.bgType;
    /** 0 = left, 1 = center, 2 = right */
    private int     textAlign       = CACHE.textAlign;
    private boolean extraCmdEnabled = CACHE.extraCmdEnabled;

    // ── Cached layout (set in init) ────────────────────────────────────────
    private int[] colLabelX   = new int[3];
    private int[] fieldStartX = new int[3];
    private int[] fieldW      = new int[3];
    private int   btnColX;

    // ── Tooltips ──────────────────────────────────────────────────────────
    private final List<int[]>  ttBounds = new ArrayList<>();
    private final List<String> ttKeys   = new ArrayList<>();

    // ── Status bar ────────────────────────────────────────────────────────
    private String statusMsg    = "";
    private long   statusExpiry = 0;

    // ── MultiLineEditBox cursor-by-click (reflection) ─────────────────────
    // We find the method by SIGNATURE rather than by name so it works regardless of
    // whether the runtime uses Mojang-mapped, SRG, or any other obfuscated name.
    // seekCursorToPoint(double mouseX, double mouseY) is the only private (DD)V
    // method declared directly in MultiLineEditBox.
    private static final java.lang.reflect.Method SEEK_CURSOR;
    static {
        java.lang.reflect.Method found = null;
        try {
            for (java.lang.reflect.Method m : MultiLineEditBox.class.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2
                        && p[0] == double.class && p[1] == double.class
                        && m.getReturnType() == void.class
                        && java.lang.reflect.Modifier.isPrivate(m.getModifiers())) {
                    m.setAccessible(true);
                    found = m;
                    break;
                }
            }
        } catch (Exception ignored) {}
        SEEK_CURSOR = found;
    }

    // ── Preview ───────────────────────────────────────────────────────────
    private TitleInstance previewInstance;
    private long lastPreviewReset = 0;

    public TitleProScreen() {
        super(Component.translatable("screen.titlepro.editor"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private EditBox mkField(int col, int y, String value) {
        EditBox f = new EditBox(this.font, fieldStartX[col], y, fieldW[col], FH, Component.empty());
        f.setMaxLength(300);
        f.setValue(value);
        this.addRenderableWidget(f);
        return f;
    }

    private void addTt(int col, int y, String key) {
        ttBounds.add(new int[]{fieldStartX[col], y, fieldW[col], FH});
        ttKeys.add(key);
    }

    private int lw(String key) {
        return this.font.width(Component.translatable(key));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Init
    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        ttBounds.clear();
        ttKeys.clear();

        int sw       = this.width;
        int textBoxW = sw - 2 * M;                       // text box spans full width
        int formW    = sw - 2 * M - BTN_GAP - BTN_COL;  // form fields stay in left area
        int colW     = formW / 3;
        btnColX      = M + formW + BTN_GAP;

        colLabelX[0] = M;
        colLabelX[1] = M + colW;
        colLabelX[2] = M + 2 * colW;

        // Max label width per column across ALL label rows
        String[][] colLabels = {
            {"titlepro.gui.label.x",      "titlepro.gui.label.stay",
             "titlepro.gui.label.bgpadx",  "titlepro.gui.label.shadow_x"},
            {"titlepro.gui.label.y",       "titlepro.gui.label.fadein",
             "titlepro.gui.label.bgcolor",  "titlepro.gui.label.bgpady",
             "titlepro.gui.label.shadow_y", "titlepro.gui.label.exitspeed"},
            {"titlepro.gui.label.scale",   "titlepro.gui.label.fadeout",
             "titlepro.gui.label.bgalpha",  "titlepro.gui.label.bgoffsety",
             "titlepro.gui.label.spacing"}
        };
        int[] maxLW = new int[3];
        for (int c = 0; c < 3; c++)
            for (String k : colLabels[c])
                maxLW[c] = Math.max(maxLW[c], lw(k));

        for (int c = 0; c < 3; c++) {
            fieldStartX[c] = colLabelX[c] + maxLW[c] + 8;
            int avail = (colLabelX[c] + colW) - fieldStartX[c] - 4;
            fieldW[c] = Math.min(MAX_FIELD_W, Math.max(28, avail));
        }

        // ── Dynamic Y positions ──────────────────────────────────────────
        y0 = TITLE_H + EXTRA_Y;
        // 18 = ROW_GAP (6) + extra 12 px for the character counter below the text box
        y1 = y0 + textBoxH + 18;
        y2 = y1 + ROW_STEP;
        y3 = y2 + ROW_STEP;
        y4 = y3 + ROW_STEP;
        y5 = y4 + ROW_STEP;
        y6 = y5 + ROW_STEP;   // left: extra command row; right: 重置内容
        y7 = y6 + ROW_STEP;   // right: 叠加方向
        y8 = y7 + ROW_STEP;   // right: 对齐方式
        y9 = y8 + ROW_STEP;   // right: 导出方案
        ySep  = y9 + FH + 5;
        yPrev = ySep + 3;

        // ── Row 0: multi-line text box (full width) ──────────────────────
        String textValue = (pendingText != null) ? pendingText : CACHE.text;
        pendingText = null;
        textBox = new MultiLineEditBox(this.font, M, y0, textBoxW, textBoxH,
                Component.translatable("titlepro.gui.hint"), Component.empty());
        textBox.setCharacterLimit(2000);
        textBox.setValue(textValue);
        this.addRenderableWidget(textBox);

        // ── Right button column ──────────────────────────────────────────
        // y1: 从配置文件中导入 (opens SchemeListScreen)
        btn("titlepro.gui.import_config", btnColX, y1, BTN_COL, FH,
            b -> openSchemeList());
        // y2: 从剪贴板导入
        btn("titlepro.gui.import_clipboard", btnColX, y2, BTN_COL, FH,
            b -> importFromClipboard());
        // y3: 预览标题
        btn("titlepro.gui.preview",          btnColX, y3, BTN_COL, FH,
            b -> refreshPreview());
        // y4: 发送给自己
        btn("titlepro.gui.send_to_self",     btnColX, y4, BTN_COL, FH,
            b -> sendToSelf());
        // y5: 生成并复制
        btn("titlepro.gui.generate_copy",    btnColX, y5, BTN_COL, FH,
            b -> generateAndCopy());
        // y6: 重置内容
        btn("titlepro.gui.reset",            btnColX, y6, BTN_COL, FH,
            b -> resetToDefaults());
        // y7: 叠加方向
        btnStack = Button.builder(
                Component.translatable(stackUp ? "titlepro.gui.stack.up" : "titlepro.gui.stack.down"),
                b -> {
                    stackUp = !stackUp;
                    b.setMessage(Component.translatable(
                            stackUp ? "titlepro.gui.stack.up" : "titlepro.gui.stack.down"));
                })
            .bounds(btnColX, y7, BTN_COL, FH).build();
        this.addRenderableWidget(btnStack);
        // y8: 对齐方式
        btnAlign = Button.builder(
                Component.translatable(alignKey(textAlign)),
                b -> {
                    textAlign = (textAlign + 1) % 3;
                    b.setMessage(Component.translatable(alignKey(textAlign)));
                })
            .bounds(btnColX, y8, BTN_COL, FH).build();
        this.addRenderableWidget(btnAlign);
        // y9: 导出方案
        btn("titlepro.gui.export_scheme",    btnColX, y9, BTN_COL, FH,
            b -> openExportDialog());

        // ── Row 1: X / Y / scale ─────────────────────────────────────────
        fieldX     = mkField(0, y1, CACHE.x);     addTt(0, y1, "titlepro.gui.tt.offset.x");
        fieldY     = mkField(1, y1, CACHE.y);     addTt(1, y1, "titlepro.gui.tt.offset.y");
        fieldScale = mkField(2, y1, CACHE.scale); addTt(2, y1, "titlepro.gui.tt.scale");

        // ── Row 2: stay / fadeIn / fadeOut ───────────────────────────────
        fieldStay    = mkField(0, y2, CACHE.stay);
        fieldFadeIn  = mkField(1, y2, CACHE.fadeIn);
        fieldFadeOut = mkField(2, y2, CACHE.fadeOut);

        // ── Row 3: [exitSlide btn] | exitSpeed | spacing ─────────────────
        int toggleW = fieldStartX[0] - colLabelX[0] + fieldW[0]; // same width as bgType btn
        btnExitSlide = Button.builder(
                Component.translatable(exitSlide ? "titlepro.gui.exitslide.on" : "titlepro.gui.exitslide.off"),
                b -> {
                    exitSlide = !exitSlide;
                    b.setMessage(Component.translatable(exitSlide
                            ? "titlepro.gui.exitslide.on" : "titlepro.gui.exitslide.off"));
                })
            .bounds(colLabelX[0], y3, toggleW, FH).build();
        this.addRenderableWidget(btnExitSlide);

        fieldExitSpeed = mkField(1, y3, CACHE.exitSpeed);
        fieldSpacing   = mkField(2, y3, CACHE.spacing); addTt(2, y3, "titlepro.gui.tt.spacing");

        // ── Row 4: [bgType btn] | bgColor | bgAlpha ──────────────────────
        btnBgType = Button.builder(
                Component.translatable(bgType == 0 ? "titlepro.gui.bgtype.shadow" : "titlepro.gui.bgtype.bg"),
                b -> {
                    saveFieldsToCache();
                    bgType = (bgType == 1) ? 0 : 1;
                    CACHE.bgType = bgType;
                    pendingText = textBox.getValue();
                    this.clearWidgets();
                    this.init();
                })
            .bounds(colLabelX[0], y4, toggleW, FH).build();
        this.addRenderableWidget(btnBgType);

        fieldBgColor = mkField(1, y4, CACHE.bgColor); addTt(1, y4, "titlepro.gui.tt.bgcolor");
        fieldBgAlpha = mkField(2, y4, CACHE.bgAlpha);

        // ── Row 5: dynamic based on bgType ───────────────────────────────
        if (bgType == 1) {
            fieldBgPadX    = mkField(0, y5, CACHE.bgPadX);    addTt(0, y5, "titlepro.gui.tt.bgpadx");
            fieldBgPadY    = mkField(1, y5, CACHE.bgPadY);    addTt(1, y5, "titlepro.gui.tt.bgpady");
            fieldBgOffsetY = mkField(2, y5, CACHE.bgOffsetY); addTt(2, y5, "titlepro.gui.tt.bgoffsety");
            fieldShadowX   = null;
            fieldShadowY   = null;
        } else {
            fieldShadowX   = mkField(0, y5, CACHE.shadowOffX); addTt(0, y5, "titlepro.gui.tt.shadow_x");
            fieldShadowY   = mkField(1, y5, CACHE.shadowOffY); addTt(1, y5, "titlepro.gui.tt.shadow_y");
            fieldBgPadX    = null;
            fieldBgPadY    = null;
            fieldBgOffsetY = null;
        }

        // ── Row 6: extra command toggle + input (full form width) ─────────
        btnExtraCmd = Button.builder(
                Component.translatable(extraCmdEnabled
                        ? "titlepro.gui.extra_cmd.on" : "titlepro.gui.extra_cmd.off"),
                b -> {
                    extraCmdEnabled = !extraCmdEnabled;
                    b.setMessage(Component.translatable(extraCmdEnabled
                            ? "titlepro.gui.extra_cmd.on" : "titlepro.gui.extra_cmd.off"));
                })
            .bounds(colLabelX[0], y6, toggleW, FH).build();
        this.addRenderableWidget(btnExtraCmd);

        int extraCmdFieldX = colLabelX[0] + toggleW + 4;
        // Align right edge with the rightmost input field in the rows above (col 2 field)
        int extraCmdFieldW = Math.max(40, fieldStartX[2] + fieldW[2] - extraCmdFieldX);
        fieldExtraCmd = new EditBox(this.font, extraCmdFieldX, y6, extraCmdFieldW, FH, Component.empty());
        fieldExtraCmd.setMaxLength(500);
        fieldExtraCmd.setValue(CACHE.extraCmd);
        this.addRenderableWidget(fieldExtraCmd);

        refreshPreview();
    }

    private Button btn(String key, int x, int y, int w, int h, Button.OnPress handler) {
        Button b = Button.builder(Component.translatable(key), handler).bounds(x, y, w, h).build();
        this.addRenderableWidget(b);
        return b;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Resize (saves field values before Minecraft clears widgets)
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void resize(Minecraft mc, int w, int h) {
        if (textBox != null) pendingText = textBox.getValue();
        saveFieldsToCache();
        super.resize(mc, w, h);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tick — auto-expand text box height
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();
        if (textBox == null || this.width == 0) return;

        int textBoxW = this.width - 2 * M;
        String tv    = textBox.getValue().replace("\n", " ").trim();
        int lineH    = this.font.lineHeight + 2;
        int lines    = tv.isEmpty() ? 1
                : this.font.split(Component.literal(tv), textBoxW - 8).size();
        int newH     = Math.max(MIN_TEXT_H, Math.min(lines * lineH + 12, MAX_TEXT_H));

        if (newH != textBoxH) {
            pendingText = textBox.getValue();
            textBoxH    = newH;
            saveFieldsToCache();
            this.clearWidgets();
            this.init();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Render
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int sw = this.width;

        g.drawCenteredString(this.font, this.title, sw / 2, 4, 0xFFFFFF);

        // Row 1 labels
        lbl(g, "titlepro.gui.label.x",     colLabelX[0], y1);
        lbl(g, "titlepro.gui.label.y",     colLabelX[1], y1);
        lbl(g, "titlepro.gui.label.scale", colLabelX[2], y1);

        // Row 2 labels
        lbl(g, "titlepro.gui.label.stay",    colLabelX[0], y2);
        lbl(g, "titlepro.gui.label.fadein",  colLabelX[1], y2);
        lbl(g, "titlepro.gui.label.fadeout", colLabelX[2], y2);

        // Row 3 labels (exitSlide is a button at col0; exitSpeed at col1; spacing at col2)
        lbl(g, "titlepro.gui.label.exitspeed", colLabelX[1], y3);
        lbl(g, "titlepro.gui.label.spacing",   colLabelX[2], y3);

        // Row 4 labels (bgType is a button; bgColor + bgAlpha have labels)
        lbl(g, "titlepro.gui.label.bgcolor",  colLabelX[1], y4);
        lbl(g, "titlepro.gui.label.bgalpha",  colLabelX[2], y4);

        // Row 5 labels — depend on bgType
        if (bgType == 1) {
            lbl(g, "titlepro.gui.label.bgpadx",    colLabelX[0], y5);
            lbl(g, "titlepro.gui.label.bgpady",    colLabelX[1], y5);
            lbl(g, "titlepro.gui.label.bgoffsety", colLabelX[2], y5);
        } else {
            lbl(g, "titlepro.gui.label.shadow_x", colLabelX[0], y5);
            lbl(g, "titlepro.gui.label.shadow_y", colLabelX[1], y5);
        }

        // Separator line + preview area
        g.fill(0, ySep, sw, ySep + 1, 0x55FFFFFF);
        int prevBottom = this.height - 12;
        if (prevBottom > yPrev + 16) {
            g.fill(0, yPrev, sw, prevBottom, 0x88000000);
            g.drawString(this.font,
                    Component.translatable("titlepro.gui.label.preview"),
                    M + 2, yPrev + 4, 0x777777, false);
            if (System.currentTimeMillis() - lastPreviewReset > 8000) refreshPreview();
            if (previewInstance != null && !previewInstance.isExpired()) {
                previewInstance.render(g, sw / 2, (yPrev + prevBottom) / 2);
            }
        }

        // Status bar
        if (System.currentTimeMillis() < statusExpiry) {
            g.drawCenteredString(this.font, statusMsg, sw / 2, this.height - 11, 0xFFFF55);
        }

        // Tooltips
        for (int i = 0; i < ttBounds.size(); i++) {
            int[] b = ttBounds.get(i);
            if (mouseX >= b[0] && mouseX < b[0] + b[2]
                    && mouseY >= b[1] && mouseY < b[1] + b[3]) {
                g.renderTooltip(this.font,
                        Component.translatable(ttKeys.get(i)), mouseX, mouseY);
                break;
            }
        }
    }

    private void lbl(GuiGraphics g, String key, int x, int y) {
        g.drawString(this.font, Component.translatable(key),
                x, y + (FH - 8) / 2, 0xCCCCCC, false);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Cache save / restore
    // ─────────────────────────────────────────────────────────────────────

    /** Save all currently-visible field values to CACHE. */
    private void saveFieldsToCache() {
        if (textBox != null)       CACHE.text       = textBox.getValue().replace("\n", "").trim();
        if (fieldX != null)        CACHE.x          = fieldX.getValue();
        if (fieldY != null)        CACHE.y          = fieldY.getValue();
        if (fieldScale != null)    CACHE.scale      = fieldScale.getValue();
        if (fieldStay != null)     CACHE.stay       = fieldStay.getValue();
        if (fieldFadeIn != null)   CACHE.fadeIn     = fieldFadeIn.getValue();
        if (fieldFadeOut != null)  CACHE.fadeOut    = fieldFadeOut.getValue();
        if (fieldSpacing != null)  CACHE.spacing    = fieldSpacing.getValue();
        if (fieldBgColor != null)  CACHE.bgColor    = fieldBgColor.getValue();
        if (fieldBgAlpha != null)  CACHE.bgAlpha    = fieldBgAlpha.getValue();
        if (fieldExitSpeed != null) CACHE.exitSpeed = fieldExitSpeed.getValue();
        // Background-only fields
        if (fieldBgPadX != null)    CACHE.bgPadX    = fieldBgPadX.getValue();
        if (fieldBgPadY != null)    CACHE.bgPadY    = fieldBgPadY.getValue();
        if (fieldBgOffsetY != null) CACHE.bgOffsetY = fieldBgOffsetY.getValue();
        // Shadow-only fields
        if (fieldShadowX != null)   CACHE.shadowOffX = fieldShadowX.getValue();
        if (fieldShadowY != null)   CACHE.shadowOffY = fieldShadowY.getValue();
        // Extra command
        if (fieldExtraCmd != null)  CACHE.extraCmd  = fieldExtraCmd.getValue();
        CACHE.exitSlide       = exitSlide;
        CACHE.stackUp         = stackUp;
        CACHE.bgType          = bgType;
        CACHE.textAlign       = textAlign;
        CACHE.extraCmdEnabled = extraCmdEnabled;
    }

    @Override
    public void removed() {
        saveFieldsToCache();
        super.removed();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Build TitleData from current field values
    // ─────────────────────────────────────────────────────────────────────

    private TitleData buildData() {
        TitleData d = new TitleData();
        String tv = textBox.getValue().replace("\n", "").trim();
        if (!tv.isEmpty()) {
            TitleProParser.ParseResult r = TitleProParser.parse(tv);
            d.text = r.valid ? r.data.text : net.minecraft.network.chat.Component.literal(tv);
        }
        d.offsetX    = parseI(fieldX, 0);
        d.offsetY    = parseI(fieldY, 0);
        d.scale      = Math.max(0.1f, parseF(fieldScale, 1f));
        d.stay       = Math.max(0, (int)(parseF(fieldStay,    5f) * 1000));
        d.fadeIn     = Math.max(0, (int)(parseF(fieldFadeIn,  0f) * 1000));
        d.fadeOut    = Math.max(0, (int)(parseF(fieldFadeOut, 1f) * 1000));
        d.spacing    = Math.max(1, parseI(fieldSpacing, 14));
        d.stackUp    = stackUp;
        d.bgType     = bgType;
        d.textAlign  = textAlign;
        try { d.bgColor = TitleProParser.parseColor(fieldBgColor.getValue().trim()); }
        catch (Exception ignored) {}
        int pct  = Math.max(0, Math.min(100, parseI(fieldBgAlpha, 50)));
        d.bgAlpha    = (int)(pct * 2.55f);
        d.exitSlide  = exitSlide;
        d.exitSpeed  = Math.max(0.1f, Math.min(10f, parseF(fieldExitSpeed, 1f)));

        if (bgType == 1) {
            d.bgPaddingX  = Math.max(0, parseI(fieldBgPadX, 3));
            d.bgPaddingY  = Math.max(0, parseI(fieldBgPadY, 1));
            d.bgOffsetY   = parseF(fieldBgOffsetY, -0.3f);
            d.shadowOffsetX = 0.4f;
            d.shadowOffsetY = 0.4f;
        } else {
            d.shadowOffsetX = Math.max(0f, parseF(fieldShadowX, 0.4f));
            d.shadowOffsetY = Math.max(0f, parseF(fieldShadowY, 0.4f));
            d.bgPaddingX  = 3;
            d.bgPaddingY  = 1;
            d.bgOffsetY   = -0.3f;
        }
        d.extraCmdEnabled = extraCmdEnabled;
        d.extraCmd        = fieldExtraCmd != null ? fieldExtraCmd.getValue().trim() : CACHE.extraCmd;
        return d;
    }

    private int   parseI(EditBox f, int def) {
        if (f == null) return def;
        String v = f.getValue().trim();
        // First try exact integer parse; fall back to float-then-truncate so that
        // a user accidentally typing "1.5" or "-0.8" gets 1 / 0 instead of the default.
        try { return Integer.parseInt(v); }
        catch (Exception e) {
            try { return (int) Float.parseFloat(v); }
            catch (Exception e2) { return def; }
        }
    }
    private float parseF(EditBox f, float def) {
        if (f == null) return def;
        try { return Float.parseFloat(f.getValue().trim()); } catch (Exception e) { return def; }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────────

    private void refreshPreview() {
        lastPreviewReset = System.currentTimeMillis();
        TitleData d = buildData();
        d.offsetX = 0;
        d.offsetY = 0;
        d.exitSlide = false;
        if (d.text.getString().isBlank()) {
            d.text = net.minecraft.network.chat.Component.literal("TitlePro");
        }
        previewInstance = new TitleInstance(d, (long) d.fadeIn);
    }

    private void generateAndCopy() {
        Minecraft.getInstance().keyboardHandler.setClipboard(TitleProParser.buildCommand(buildData()));
        setStatus(Component.translatable("titlepro.gui.status.copied").getString());
    }

    private void sendToSelf() {
        TitleRenderer.add(buildData());
        // Execute optional extra command client-side
        if (extraCmdEnabled && fieldExtraCmd != null) {
            String cmd = fieldExtraCmd.getValue().trim();
            if (!cmd.isBlank() && Minecraft.getInstance().player != null) {
                String bare = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                try {
                    Minecraft.getInstance().player.connection.sendCommand(bare);
                } catch (Exception ignored) {}
            }
        }
        setStatus(Component.translatable("titlepro.gui.status.displayed").getString());
    }

    private void importFromClipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clip == null || clip.isBlank()) {
            setStatus(Component.translatable("titlepro.gui.status.clipboard_empty").getString());
            return;
        }
        String args = clip.trim();
        // Strip "/titlepro " prefix then any "@selector" token
        if (args.toLowerCase().startsWith("/titlepro ")) {
            args = args.substring(10).trim();
            if (args.startsWith("@")) {
                int sp = args.indexOf(' ');
                if (sp > 0) args = args.substring(sp + 1).trim();
            }
        }
        TitleProParser.ParseResult r = TitleProParser.parse(args);
        if (!r.valid) { setStatus("§c" + r.error); return; }
        applyToFields(r.data);
        setStatus(Component.translatable("titlepro.gui.status.imported").getString());
    }

    /** Open the SchemeListScreen. The callback writes CACHE and returns here via setScreen(). */
    private void openSchemeList() {
        saveFieldsToCache();
        minecraft.setScreen(new SchemeListScreen(this, sd -> {
            // Populate CACHE from the loaded scheme so init() will read them
            writeDataToCache(sd.data);
            // Update instance vars that init() reads (not from CACHE)
            bgType          = sd.data.bgType;
            exitSlide       = sd.data.exitSlide;
            stackUp         = sd.data.stackUp;
            textAlign       = sd.data.textAlign;
            extraCmdEnabled = sd.data.extraCmdEnabled;
            textBoxH        = MIN_TEXT_H;
            pendingText     = null;
            // Return to this screen — Minecraft calls init() which reads CACHE
            minecraft.setScreen(TitleProScreen.this);
            setStatus(Component.translatable("titlepro.gui.status.scheme_imported").getString());
        }));
    }

    /**
     * Write all TitleData fields into CACHE without triggering a widget rebuild.
     * Used when switching screens: the caller is responsible for calling init().
     */
    private void writeDataToCache(TitleData d) {
        try {
            // Escape § → \u00a7 so the textBox (which strips § as a forbidden chat char)
            // stores a safe ASCII sequence, and the JSON round-trip remains correct.
            CACHE.text = net.minecraft.network.chat.Component.Serializer.toJson(d.text)
                    .replace("§", "\\u00a7");
        } catch (Exception e) {
            CACHE.text = d.text.getString().replace("§", "\\u00a7");
        }
        CACHE.x              = String.valueOf(d.offsetX);
        CACHE.y              = String.valueOf(d.offsetY);
        CACHE.scale          = fmt1(d.scale);
        CACHE.stay           = fmt1(d.stay    / 1000f);
        CACHE.fadeIn         = fmt1(d.fadeIn  / 1000f);
        CACHE.fadeOut        = fmt1(d.fadeOut / 1000f);
        CACHE.spacing        = String.valueOf(d.spacing);
        CACHE.bgColor        = String.format("#%06X", d.bgColor & 0xFFFFFF);
        CACHE.bgAlpha        = String.valueOf(Math.round(d.bgAlpha / 2.55f));
        CACHE.bgPadX         = String.valueOf(d.bgPaddingX);
        CACHE.bgPadY         = String.valueOf(d.bgPaddingY);
        CACHE.bgOffsetY      = String.valueOf(d.bgOffsetY);
        CACHE.shadowOffX     = fmt1(d.shadowOffsetX);
        CACHE.shadowOffY     = fmt1(d.shadowOffsetY);
        CACHE.exitSpeed      = fmt1(d.exitSpeed);
        CACHE.exitSlide      = d.exitSlide;
        CACHE.stackUp        = d.stackUp;
        CACHE.bgType         = d.bgType;
        CACHE.textAlign      = d.textAlign;
        CACHE.extraCmdEnabled = d.extraCmdEnabled;
        CACHE.extraCmd       = d.extraCmd != null ? d.extraCmd : "";
    }

    /** Open the ExportSchemeScreen dialog. */
    private void openExportDialog() {
        saveFieldsToCache();
        String extraCmdStr = fieldExtraCmd != null ? fieldExtraCmd.getValue() : CACHE.extraCmd;
        minecraft.setScreen(new ExportSchemeScreen(this, buildData(), extraCmdEnabled, extraCmdStr));
    }

    private void resetToDefaults() {
        CACHE.text          = "{\"extra\":[{\"bold\":false,\"italic\":false,\"color\":\"#FFAA00\",\"text\":\"Title\"},{\"bold\":false,\"italic\":false,\"color\":\"#55FFFF\",\"text\":\"\\u00a7cP\\u00a7a\\u00a7or\\u00a7ro\"}],\"text\":\"\"}";
        CACHE.x = "0"; CACHE.y = "0"; CACHE.scale = "1.0";
        CACHE.stay = "5.0"; CACHE.fadeIn = "0.0"; CACHE.fadeOut = "1.0";
        CACHE.spacing       = "14";
        CACHE.bgColor       = "#000000"; CACHE.bgAlpha = "50";
        CACHE.bgPadX = "3"; CACHE.bgPadY = "1"; CACHE.bgOffsetY = "-0.3";
        CACHE.shadowOffX    = "0.4"; CACHE.shadowOffY = "0.4";
        CACHE.exitSpeed     = "1.0";
        CACHE.exitSlide     = true; CACHE.stackUp = false; CACHE.bgType = 1; CACHE.textAlign = 1;
        CACHE.extraCmdEnabled = false; CACHE.extraCmd = "";
        bgType = 1; exitSlide = true; stackUp = false; textAlign = 1; extraCmdEnabled = false;
        pendingText = null;
        textBoxH = MIN_TEXT_H;
        this.clearWidgets();
        this.init();
        setStatus(Component.translatable("titlepro.gui.status.reset").getString());
    }

    /**
     * Apply imported TitleData to CACHE, update local state, then rebuild widgets.
     * Uses the JSON representation so styled components are preserved in the text box.
     */
    private void applyToFields(TitleData d) {
        writeDataToCache(d);
        bgType          = d.bgType;
        exitSlide       = d.exitSlide;
        stackUp         = d.stackUp;
        textAlign       = d.textAlign;
        extraCmdEnabled = d.extraCmdEnabled;
        pendingText     = null;
        textBoxH        = MIN_TEXT_H;
        this.clearWidgets();
        this.init();
        refreshPreview();
    }

    /** Package-private so ExportSchemeScreen can pass success messages back. */
    void setStatus(String msg) { statusMsg = msg; statusExpiry = System.currentTimeMillis() + 3000; }

    private String fmt1(float v) { return String.format("%.1f", v); }

    private static String alignKey(int a) {
        return switch (a) {
            case 0 -> "titlepro.gui.align.left";
            case 2 -> "titlepro.gui.align.right";
            default -> "titlepro.gui.align.center";
        };
    }

    /**
     * Override mouseClicked to allow clicking inside the MultiLineEditBox to
     * reposition the text cursor. The vanilla widget handles focus/selection but
     * does NOT call the private seekCursorToPoint() from mouseClicked, so we
     * invoke it via reflection after super handles the event.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && textBox != null && textBox.isMouseOver(mouseX, mouseY)) {
            // Ensure screen-level focus so keyboard events reach the textBox,
            // then seek cursor to click position regardless of vanilla isHovered() state.
            if (this.getFocused() != textBox) {
                this.setFocused(textBox);
            }
            if (SEEK_CURSOR != null) {
                try { SEEK_CURSOR.invoke(textBox, mouseX, mouseY); }
                catch (Exception ignored) {}
            }
            return true;
        }
        return result;
    }

    @Override public boolean isPauseScreen() { return false; }
}
