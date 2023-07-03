package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LoadedChunkViewPacket extends BaseS2CMessage {
	public static final int LOADED = 1;
	public static final int FORCE_LOADED = 2;

	private final ResourceKey<Level> dimension;
	private final Long2IntMap chunks;

	public LoadedChunkViewPacket(ResourceKey<Level> dimension, Long2IntMap chunks) {
		this.dimension = dimension;
		this.chunks = chunks;
	}

	LoadedChunkViewPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
		chunks = buf.readMap(Long2IntOpenHashMap::new, FriendlyByteBuf::readLong, FriendlyByteBuf::readVarInt);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.LOADED_CHUNK_VIEW;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeMap(chunks, FriendlyByteBuf::writeLong, FriendlyByteBuf::writeVarInt);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunksClient.syncLoadedChunkViewFromServer(dimension, chunks);
	}
}