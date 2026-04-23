package com.xioyim.titlepro.client;

import com.xioyim.titlepro.data.TitleData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TitleRenderer {

    private static final List<TitleInstance> INSTANCES = new ArrayList<>();

    /**
     * Cumulative pixel offset for the next title to be added.
     * Grows each time a title is added (in the configured stack direction).
     * Resets to 0 only when the INSTANCES list is completely empty —
     * i.e. all previous titles have fully expired and been removed.
     * This means new titles always stack relative to the last title's
     * position, regardless of whether earlier titles have already faded out.
     */
    private static float nextOffset = 0f;

    public static void add(TitleData data) {
        // Reset stacking position only when the screen is completely clear.
        if (INSTANCES.isEmpty()) {
            nextOffset = 0f;
        }

        float thisOffset = nextOffset;
        float dir = data.stackUp ? -1f : 1f;
        nextOffset += data.spacing * dir;

        INSTANCES.add(new TitleInstance(data, thisOffset));
    }

    public static void clear() {
        INSTANCES.clear();
        nextOffset = 0f;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return; // not in a world

        INSTANCES.removeIf(TitleInstance::isExpired);
        if (INSTANCES.isEmpty()) return;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;

        for (TitleInstance inst : INSTANCES) {
            inst.render(event.getGuiGraphics(), cx, cy);
        }
    }
}
