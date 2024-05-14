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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public record SendManyChunksPacket(ResourceKey<Level> dimension, UUID teamId, List<ChunkSyncInfo> chunks) implements CustomPacketPayload {
	public static final Type<SendManyChunksPacket> TYPE = new Type<>(FTBChunksAPI.rl("send_many_chunks_packet"));

	public static final StreamCodec<FriendlyByteBuf, SendManyChunksPacket> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION), SendManyChunksPacket::dimension,
			UUIDUtil.STREAM_CODEC, SendManyChunksPacket::teamId,
			ChunkSyncInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), SendManyChunksPacket::chunks,
			SendManyChunksPacket::new
	);

	public void sendToAll(MinecraftServer server, ChunkTeamData teamData) {
        if (teamData.shouldHideClaims()) {
            SendManyChunksPacket hiddenPacket = hidden();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(player, teamData.isAlly(player.getUUID()) ? this : hiddenPacket);
            }
        } else {
            NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), this);
        }
    }

	public void sendToPlayer(ServerPlayer player, ChunkTeamData teamData) {
        if (teamData.shouldHideClaims()) {
            NetworkManager.sendToPlayer(player, teamData.isAlly(player.getUUID()) ? this : hidden());
        } else {
            NetworkManager.sendToPlayer(player, this);
        }
    }

	public SendManyChunksPacket hidden() {
		// shallow copy of packet.chunks OK here, it only contains primitive elements
		return new SendManyChunksPacket(dimension(), Util.NIL_UUID, chunks().stream().map(ChunkSyncInfo::hidden).toList());
	}

	@Override
	public Type<SendManyChunksPacket> type() {
		return TYPE;
	}

	public static void handle(SendManyChunksPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBChunksClient.INSTANCE.updateChunksFromServer(message.dimension, message.teamId, message.chunks));
	}
}