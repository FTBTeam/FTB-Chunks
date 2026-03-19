package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientNet;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public enum RequestBlockColorPacket implements CustomPacketPayload {
    INSTANCE;

    public static final Type<RequestBlockColorPacket> TYPE = new Type<>(FTBChunksAPI.id("request_block_color_packet"));

    public static final StreamCodec<FriendlyByteBuf, RequestBlockColorPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<RequestBlockColorPacket> type() {
        return TYPE;
    }

    public static void handle(RequestBlockColorPacket message, PacketContext context) {
        context.enqueue(FTBChunksClientNet::handleBlockColorRequestPacket);
    }
}
