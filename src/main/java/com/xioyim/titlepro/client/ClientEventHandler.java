package com.xioyim.titlepro.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {

    public static final KeyMapping OPEN_EDITOR_KEY = new KeyMapping(
        "key.titlepro.open_editor",
        GLFW.GLFW_KEY_I,
        "key.categories.titlepro"
    );

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_EDITOR_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new TitleProScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("titleproeditor")
                .executes(ctx -> {
                    Minecraft.getInstance().execute(() ->
                        Minecraft.getInstance().setScreen(new TitleProScreen()));
                    return 1;
                })
        );
    }
}
