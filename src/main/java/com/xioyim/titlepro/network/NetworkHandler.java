package com.xioyim.titlepro.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("titlepro", "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0,
            TitleProPacket.class,
            TitleProPacket::encode,
            TitleProPacket::decode,
            TitleProPacket::handle,
            java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendToPlayer(ServerPlayer player, TitleProPacket pkt) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), pkt);
    }
}
