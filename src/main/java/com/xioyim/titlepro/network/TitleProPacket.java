package com.xioyim.titlepro.network;

import com.xioyim.titlepro.client.TitleRenderer;
import com.xioyim.titlepro.data.TitleData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TitleProPacket {

    public final TitleData data;

    public TitleProPacket(TitleData data) {
        this.data = data;
    }

    public static void encode(TitleProPacket pkt, FriendlyByteBuf buf) {
        buf.writeComponent(pkt.data.text);
        buf.writeInt(pkt.data.offsetX);
        buf.writeInt(pkt.data.offsetY);
        buf.writeFloat(pkt.data.scale);
        buf.writeInt(pkt.data.stay);
        buf.writeInt(pkt.data.fadeIn);
        buf.writeInt(pkt.data.fadeOut);
        buf.writeInt(pkt.data.spacing);
        buf.writeBoolean(pkt.data.stackUp);
        buf.writeInt(pkt.data.bgType);
        buf.writeInt(pkt.data.bgColor);
        buf.writeInt(pkt.data.bgAlpha);
        buf.writeInt(pkt.data.bgPaddingX);
        buf.writeInt(pkt.data.bgPaddingY);
        buf.writeFloat(pkt.data.bgOffsetY);
        buf.writeFloat(pkt.data.shadowOffsetX);
        buf.writeFloat(pkt.data.shadowOffsetY);
        buf.writeBoolean(pkt.data.exitSlide);
        buf.writeFloat(pkt.data.exitSpeed);
        buf.writeInt(pkt.data.textAlign);
    }

    public static TitleProPacket decode(FriendlyByteBuf buf) {
        TitleData d = new TitleData();
        d.text = buf.readComponent();
        d.offsetX = buf.readInt();
        d.offsetY = buf.readInt();
        d.scale = buf.readFloat();
        d.stay = buf.readInt();
        d.fadeIn = buf.readInt();
        d.fadeOut = buf.readInt();
        d.spacing = buf.readInt();
        d.stackUp = buf.readBoolean();
        d.bgType = buf.readInt();
        d.bgColor = buf.readInt();
        d.bgAlpha = buf.readInt();
        d.bgPaddingX = buf.readInt();
        d.bgPaddingY = buf.readInt();
        d.bgOffsetY = buf.readFloat();
        d.shadowOffsetX = buf.readFloat();
        d.shadowOffsetY = buf.readFloat();
        d.exitSlide = buf.readBoolean();
        d.exitSpeed = buf.readFloat();
        d.textAlign = buf.readInt();
        return new TitleProPacket(d);
    }

    public static void handle(TitleProPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> TitleRenderer.add(pkt.data));
        ctx.get().setPacketHandled(true);
    }
}
