package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientNet;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SyncRXPacket(RegionSyncKey key, int offset, int total, byte[] data) implements CustomPacketPayload {
	public static final Type<SyncRXPacket> TYPE = new Type<>(FTBChunksAPI.id("sync_rx_packet"));

	public static final StreamCodec<FriendlyByteBuf, SyncRXPacket> STREAM_CODEC = StreamCodec.composite(
			RegionSyncKey.STREAM_CODEC, SyncRXPacket::key,
			ByteBufCodecs.VAR_INT, SyncRXPacket::offset,
			ByteBufCodecs.VAR_INT, SyncRXPacket::total,
			ByteBufCodecs.BYTE_ARRAY, SyncRXPacket::data,
			SyncRXPacket::new
	);

	@Override
	public Type<SyncRXPacket> type() {
		return TYPE;
	}

	public static void handle(SyncRXPacket message, PacketContext context) {
		FTBChunksClientNet.handleSyncRegionFromServer(message.key, message.offset, message.total, message.data);
	}
}
