package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RequestBlockColorPacket() implements CustomPacketPayload {
    public static final Type<RequestBlockColorPacket> TYPE = new Type<>(FTBChunksAPI.rl("request_block_color_packet"));

    private static final RequestBlockColorPacket INSTANCE = new RequestBlockColorPacket();

    public static final StreamCodec<FriendlyByteBuf, RequestBlockColorPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<RequestBlockColorPacket> type() {
        return TYPE;
    }

    public static void handle(RequestBlockColorPacket message, NetworkManager.PacketContext context) {
        context.queue(FTBChunksClient.INSTANCE::handleBlockColorRequest);
    }
}
