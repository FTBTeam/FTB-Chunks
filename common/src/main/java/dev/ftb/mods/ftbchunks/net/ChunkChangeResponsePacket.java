package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;

public record ChunkChangeResponsePacket(
        int totalChunks, int changedChunks, Map<String,Integer> problems
) implements CustomPacketPayload
{
    public static final Type<ChunkChangeResponsePacket> TYPE = new Type<>(FTBChunksAPI.rl("chunk_change_response_packet"));

    public static final StreamCodec<FriendlyByteBuf, ChunkChangeResponsePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChunkChangeResponsePacket::totalChunks,
            ByteBufCodecs.VAR_INT, ChunkChangeResponsePacket::changedChunks,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), ChunkChangeResponsePacket::problems,
            ChunkChangeResponsePacket::new
    );

    @Override
    public Type<ChunkChangeResponsePacket> type() {
        return TYPE;
    }

    public static void handle(ChunkChangeResponsePacket message, NetworkManager.PacketContext context) {
        context.queue(() -> ChunkScreenPanel.notifyChunkUpdates(message.totalChunks, message.changedChunks, message.problems));
    }
}
