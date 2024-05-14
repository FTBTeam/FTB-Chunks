package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record LoadedChunkViewPacket(ResourceKey<Level> dimension, Long2IntMap chunks) implements CustomPacketPayload {
	public static final Type<LoadedChunkViewPacket> TYPE = new Type<>(FTBChunksAPI.rl("loaded_chunk_view_packet"));

	public static final StreamCodec<FriendlyByteBuf, LoadedChunkViewPacket> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION), LoadedChunkViewPacket::dimension,
			ByteBufCodecs.map(Long2IntOpenHashMap::new, ByteBufCodecs.VAR_LONG, ByteBufCodecs.INT), LoadedChunkViewPacket::chunks,
			LoadedChunkViewPacket::new
	);

	public static final int LOADED = 1;
	public static final int FORCE_LOADED = 2;

	@Override
	public Type<LoadedChunkViewPacket> type() {
		return TYPE;
	}

	public static void handle(LoadedChunkViewPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBChunksClient.INSTANCE.syncLoadedChunkViewFromServer(message.dimension, message.chunks));
	}
}