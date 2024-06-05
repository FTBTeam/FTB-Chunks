package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.data.ChunkSyncInfo;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public record SendChunkPacket(ResourceKey<Level> dimension, UUID teamId, ChunkSyncInfo chunk) implements CustomPacketPayload {
	public static final Type<SendChunkPacket> TYPE = new Type<>(FTBChunksAPI.rl("send_chunk_packet"));

	public static final StreamCodec<FriendlyByteBuf, SendChunkPacket> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION), SendChunkPacket::dimension,
			UUIDUtil.STREAM_CODEC, SendChunkPacket::teamId,
			ChunkSyncInfo.STREAM_CODEC, SendChunkPacket::chunk,
			SendChunkPacket::new
	);

	public void sendToAll(MinecraftServer server, ChunkTeamData teamData) {
        if (teamData.shouldHideClaims()) {
			// only send to those players who are allies of the team
            SendChunkPacket hiddenPacket = new SendChunkPacket(dimension(), Util.NIL_UUID, chunk().hidden());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, teamData.isAlly(player.getUUID()) ? this : hiddenPacket);
            }
        } else {
			// just send to everyone
            NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), this);
        }
    }

	@Override
	public Type<SendChunkPacket> type() {
		return TYPE;
	}

	public static void handle(SendChunkPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBChunksClient.INSTANCE.updateChunksFromServer(message.dimension, message.teamId, List.of(message.chunk)));
	}
}