package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ServerConfigResponsePacket(SNBTCompoundTag config) implements CustomPacketPayload {
    public static final Type<ServerConfigResponsePacket> TYPE = new Type<>(FTBChunksAPI.rl("server_config_response_packet"));

    public static final StreamCodec<FriendlyByteBuf, ServerConfigResponsePacket> STREAM_CODEC = StreamCodec.composite(
	    SNBTCompoundTag.STREAM_CODEC, ServerConfigResponsePacket::config,
	    ServerConfigResponsePacket::new
    );

    @Override
    public Type<ServerConfigResponsePacket> type() {
        return TYPE;
    }

    public static void handle(ServerConfigResponsePacket message, NetworkManager.PacketContext context) {
        FTBChunks.LOGGER.info("Received FTB Chunks server config from server");
        context.queue(() -> FTBChunksWorldConfig.CONFIG.read(message.config));
    }
}
