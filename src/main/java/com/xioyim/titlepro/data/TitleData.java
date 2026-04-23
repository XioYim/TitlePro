package com.xioyim.titlepro.data;

import net.minecraft.network.chat.Component;

public class TitleData {
    public Component text = Component.literal("");
    public int offsetX = 0;
    public int offsetY = 0;
    public float scale = 1.0f;       // text size multiplier
    public int stay = 5000;          // ms
    public int fadeIn = 0;           // ms
    public int fadeOut = 0;          // ms
    public int spacing = 14;         // px between stacked titles
    public boolean stackUp = false;
    /** 0 = shadow mode, 1 = background mode */
    public int bgType = 1;
    public int bgColor = 0x000000;
    public int bgAlpha = 127;        // 0-255 (50% ≈ 127)
    public int bgPaddingX = 3;       // horizontal padding left+right of text
    public int bgPaddingY = 1;       // vertical padding above+below text
    public float bgOffsetY = -0.3f;   // background vertical offset from text position
    /** Shadow X offset in pixels (used when bgType == 0) */
    public float shadowOffsetX = 0.4f;
    /** Shadow Y offset in pixels (used when bgType == 0) */
    public float shadowOffsetY = 0.4f;
    public boolean exitSlide = true;    // whether to play the upward slide on exit
    public float exitSpeed = 1.0f;      // multiplier for exit slide speed (0.1 ~ 10.0)
    /** 0 = left, 1 = center, 2 = right — anchor is offsetX */
    public int textAlign = 1;
    /** When true, extraCmd is executed server-side alongside this title */
    public boolean extraCmdEnabled = false;
    /** Arbitrary command executed when this title is shown (server-side) */
    public String  extraCmd = "";

    public TitleData() {}

    public TitleData copy() {
        TitleData d = new TitleData();
        d.text = text;
        d.offsetX = offsetX;
        d.offsetY = offsetY;
        d.scale = scale;
        d.stay = stay;
        d.fadeIn = fadeIn;
        d.fadeOut = fadeOut;
        d.spacing = spacing;
        d.stackUp = stackUp;
        d.bgType = bgType;
        d.bgColor = bgColor;
        d.bgAlpha = bgAlpha;
        d.bgPaddingX = bgPaddingX;
        d.bgPaddingY = bgPaddingY;
        d.bgOffsetY = bgOffsetY;
        d.shadowOffsetX = shadowOffsetX;
        d.shadowOffsetY = shadowOffsetY;
        d.exitSlide       = exitSlide;
        d.exitSpeed       = exitSpeed;
        d.textAlign       = textAlign;
        d.extraCmdEnabled = extraCmdEnabled;
        d.extraCmd        = extraCmd;
        return d;
    }
}
