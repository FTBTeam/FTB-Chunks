package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record SyncTXPacket(RegionSyncKey key, int offset, int total, byte[] data) implements CustomPacketPayload {
	public static final Type<SyncTXPacket> TYPE = new Type<>(FTBChunksAPI.rl("sync_tx_packet"));

	public static final StreamCodec<FriendlyByteBuf, SyncTXPacket> STREAM_CODEC = StreamCodec.composite(
			RegionSyncKey.STREAM_CODEC, SyncTXPacket::key,
			ByteBufCodecs.VAR_INT, SyncTXPacket::offset,
			ByteBufCodecs.VAR_INT, SyncTXPacket::total,
			ByteBufCodecs.BYTE_ARRAY, SyncTXPacket::data,
			SyncTXPacket::new
	);

	@Override
	public Type<SyncTXPacket> type() {
		return TYPE;
	}

	public static void handle(SyncTXPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> {
			ServerPlayer serverPlayer = (ServerPlayer) context.getPlayer();
			ChunkTeamDataImpl teamData = ClaimedChunkManagerImpl.getInstance().getOrCreateData(serverPlayer);

			for (ServerPlayer p1 : serverPlayer.getServer().getPlayerList().getPlayers()) {
				if (p1 != serverPlayer && teamData.isAlly(serverPlayer.getUUID())) {
					NetworkManager.sendToPlayer(p1, message);
				}
			}
		});
	}
}