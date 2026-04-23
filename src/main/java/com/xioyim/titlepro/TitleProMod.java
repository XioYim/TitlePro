package com.xioyim.titlepro;

import com.xioyim.titlepro.client.ClientEventHandler;
import com.xioyim.titlepro.config.TitleProConfig;
import com.xioyim.titlepro.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(TitleProMod.MOD_ID)
public class TitleProMod {

    public static final String MOD_ID = "titlepro";

    public TitleProMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TitleProConfig.SPEC, "titlepro/titlepro-common.toml");

        // Register network
        NetworkHandler.register();

        // Register client-only events on mod bus
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(TitleProMod::onRegisterKeyMappings);
        }
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientEventHandler.OPEN_EDITOR_KEY);
    }
}
