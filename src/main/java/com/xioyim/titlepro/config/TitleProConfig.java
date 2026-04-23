package com.xioyim.titlepro.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TitleProConfig {

    public static final ForgeConfigSpec SPEC;
    public static final TitleProConfig INSTANCE;

    static {
        Pair<TitleProConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(TitleProConfig::new);
        SPEC = pair.getRight();
        INSTANCE = pair.getLeft();
    }

    public final ForgeConfigSpec.IntValue bgPaddingHeight;
    public final ForgeConfigSpec.IntValue bgPaddingWidth;
    public final ForgeConfigSpec.DoubleValue exitAnimationSpeed;
    public final ForgeConfigSpec.BooleanValue exitAnimationEnabled;
    public final ForgeConfigSpec.IntValue wrapWidth;

    public TitleProConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("TitlePro Configuration").push("general");

        bgPaddingHeight = builder
            .comment("Background box vertical padding (pixels above and below text)")
            .defineInRange("bgPaddingHeight", 2, 0, 32);

        bgPaddingWidth = builder
            .comment("Background box horizontal padding (pixels left and right of text)")
            .defineInRange("bgPaddingWidth", 5, 0, 64);

        exitAnimationSpeed = builder
            .comment("Speed multiplier for the exit (upward slide) animation")
            .defineInRange("exitAnimationSpeed", 1.0, 0.1, 10.0);

        exitAnimationEnabled = builder
            .comment("Whether to play the upward slide animation when titles expire")
            .define("exitAnimationEnabled", true);

        wrapWidth = builder
            .comment("Maximum pixel width before text wraps to a new line.",
                     "Increase this to allow more text per line. Decrease for narrower text blocks.",
                     "Range: 50 ~ 2000  |  Default: 1000")
            .defineInRange("wrapWidth", 1000, 50, 2000);

        builder.pop();
    }
}
