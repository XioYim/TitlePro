package com.xioyim.titlepro.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xioyim.titlepro.config.TitleProConfig;
import com.xioyim.titlepro.data.TitleData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public class TitleInstance {

    private final TitleData data;
    private final long startMs;
    /**
     * Pixel offset from the base Y position, locked at creation time.
     * Positive = downward, negative = upward (if stackUp).
     * Never changes after add().
     */
    private final float lockedYOffset;

    /** Normal constructor — called by TitleRenderer.add() with the pre-computed pixel offset. */
    public TitleInstance(TitleData data, float lockedYOffset) {
        this.data = data;
        this.lockedYOffset = lockedYOffset;
        this.startMs = System.currentTimeMillis();
    }

    /** Preview constructor: startOffset ms already elapsed, always at offset 0. */
    public TitleInstance(TitleData data, long startOffset) {
        this.data = data;
        this.lockedYOffset = 0f;
        this.startMs = System.currentTimeMillis() - startOffset;
    }

    public boolean isExpired() {
        return elapsed() >= (long) data.stay + data.fadeOut;
    }

    /** True once this title has entered its fade-out / exit phase. */
    public boolean isFadingOut() {
        return elapsed() > data.stay;
    }

    private long elapsed() {
        return System.currentTimeMillis() - startMs;
    }

    /**
     * Render this title instance.
     * Uses the locked Y offset — position never changes after creation.
     */
    public void render(GuiGraphics g, int cx, int cy) {
        Font font = Minecraft.getInstance().font;
        final long t = elapsed();
        final long totalDur = (long) data.stay + data.fadeOut;
        if (t >= totalDur) return;  // hard guard: never render past deadline

        float alpha = Mth.clamp(getAlpha(t), 0f, 1f);
        if (alpha < 0.02f) return;

        // Base Y — use LOCKED offset, never the current list position
        float baseY = cy + data.offsetY + lockedYOffset;

        // Exit animation: slide upward during fadeOut (if exitSlide enabled)
        if (data.exitSlide && data.fadeOut > 0 && t > data.stay) {
            float progress = (t - data.stay) / (float) data.fadeOut;
            baseY -= progress * 20f * Mth.clamp(data.exitSpeed, 0.1f, 10f);
        }

        // Use player-configurable wrap width
        int maxWidth = TitleProConfig.INSTANCE.wrapWidth.get();
        List<FormattedCharSequence> lines = font.split(data.text, maxWidth);
        int lineH = font.lineHeight;
        int totalH = lines.isEmpty() ? lineH : lineH * lines.size() + (lines.size() - 1) * 2;

        // Max rendered line width
        int textW = 0;
        for (FormattedCharSequence line : lines) {
            textW = Math.max(textW, font.width(line));
        }

        int alphaInt = (int)(alpha * 255) & 0xFF;
        float s = Math.max(0.1f, data.scale);

        // Ensure alpha blending is active so ARGB alpha is respected
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int textColor = (alphaInt << 24) | 0xFFFFFF;
        float scaledHalfH = (totalH * s) / 2f;

        if (data.bgType == 0) {
            // ── Shadow mode ──────────────────────────────────────────────────
            //
            // Shadow-specific fade threshold (0.04f) is deliberately HIGHER than the
            // text threshold (0.02f).  Reason: at very low alpha a dark shadow on a
            // bright background retains visible contrast long after the main text has
            // become perceptually invisible — causing the "shadow outlasts text by one
            // frame" artefact.  By clearing the shadow slightly earlier we guarantee
            // the dark blot is gone before (or at the same time as) the text.
            float shadowAlphaF = alpha * (data.bgAlpha / 255f);
            if (shadowAlphaF >= 0.04f) {
                int shadowA = (int)(shadowAlphaF * 255) & 0xFF;
                int shadowColor = (shadowA << 24) | (data.bgColor & 0xFFFFFF);

                // We need the shadow to appear in shadowColor regardless of how the
                // source text is styled.  Two things can override the color argument
                // in drawString():
                //
                //   1. Per-character style colors from a JSON component like
                //      {"extra":[{"text":"X","color":"yellow"}]} — these are stripped
                //      automatically because getString() returns only the raw content
                //      ("X"), and Component.literal() creates an unstyled component.
                //
                //   2. Legacy § formatting codes embedded in the text string, e.g.
                //      {"text":"§eHello"}.  getString() preserves those literal §
                //      characters, so Component.literal("§eHello") would cause the
                //      font renderer to re-apply the yellow colour.  We strip them
                //      explicitly with ChatFormatting.stripFormatting().
                String rawShadowText = ChatFormatting.stripFormatting(
                        data.text.getString());
                if (rawShadowText == null) rawShadowText = "";
                List<FormattedCharSequence> shadowLines = font.split(
                        Component.literal(rawShadowText), maxWidth);

                g.pose().pushPose();
                g.pose().translate(
                    cx + data.offsetX + data.shadowOffsetX,
                    baseY - scaledHalfH + data.shadowOffsetY, 0);
                if (s != 1f) g.pose().scale(s, s, 1f);
                int dy = 0;
                for (FormattedCharSequence shadowLine : shadowLines) {
                    int lineW = font.width(shadowLine);
                    g.drawString(font, shadowLine, alignX(lineW), dy, shadowColor, false);
                    dy += lineH + 2;
                }
                g.pose().popPose();
            }

            // Draw main text (no background box)
            g.pose().pushPose();
            g.pose().translate(cx + data.offsetX, baseY - scaledHalfH, 0);
            if (s != 1f) g.pose().scale(s, s, 1f);
            int dy = 0;
            for (FormattedCharSequence line : lines) {
                int lineW = font.width(line);
                g.drawString(font, line, alignX(lineW), dy, textColor, false);
                dy += lineH + 2;
            }
            g.pose().popPose();

        } else {
            // ── Background mode ──────────────────────────────────────────────
            int px = data.bgPaddingX;
            int py = data.bgPaddingY;
            int bgAlphaInt = (int)(alpha * data.bgAlpha) & 0xFF;
            int bgArgb = (bgAlphaInt << 24) | (data.bgColor & 0xFFFFFF);

            g.pose().pushPose();
            // Translate so that the scaled text block is visually centered at baseY.
            g.pose().translate(cx + data.offsetX, baseY - scaledHalfH, 0);
            if (s != 1f) g.pose().scale(s, s, 1f);

            // Background box — bounds depend on textAlign; bgOffsetY shifts vertically
            g.pose().pushPose();
            g.pose().translate(0f, data.bgOffsetY, 0f);
            int boxL, boxR;
            if (data.textAlign == 0) { boxL = -px;           boxR = textW + px; }
            else if (data.textAlign == 2) { boxL = -textW - px; boxR = px; }
            else                    { boxL = -textW / 2 - px; boxR = textW / 2 + px; }
            g.fill(boxL, -py, boxR, totalH + py, bgArgb);
            g.pose().popPose();

            // Text lines
            int dy = 0;
            for (FormattedCharSequence line : lines) {
                int lineW = font.width(line);
                g.drawString(font, line, alignX(lineW), dy, textColor, false);
                dy += lineH + 2;
            }
            g.pose().popPose();
        }
    }

    private float getAlpha(long t) {
        if (t >= (long) data.stay + data.fadeOut) return 0f;

        float alpha = 1f;

        // Fade-in: ramp 0→1 over [0, fadeIn)
        if (data.fadeIn > 0 && t < data.fadeIn) {
            alpha = Math.max(0f, t / (float) data.fadeIn);
        }

        // Fade-out: ramp 1→0 over (stay, stay+fadeOut]
        // Use min() so that when fadeIn > stay the two curves overlap smoothly
        if (data.fadeOut > 0 && t > data.stay) {
            float fo = Math.max(0f, 1f - (t - data.stay) / (float) data.fadeOut);
            alpha = Math.min(alpha, fo);
        }

        return alpha;
    }

    /** X draw offset for a line of given width, based on data.textAlign. */
    private int alignX(int lineW) {
        return switch (data.textAlign) {
            case 0  -> 0;        // left: anchor = left edge
            case 2  -> -lineW;   // right: anchor = right edge
            default -> -lineW / 2; // center
        };
    }

    public TitleData getData() { return data; }
}
