package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class LoadedChunkViewPacket extends BaseS2CMessage {
	public final ResourceKey<Level> dimension;
	public final Long2IntMap chunks;

	public LoadedChunkViewPacket(ResourceKey<Level> d, Long2IntMap c) {
		dimension = d;
		chunks = c;
	}

	LoadedChunkViewPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		int s = buf.readVarInt();

		chunks = new Long2IntOpenHashMap(s);

		for (int i = 0; i < s; i++) {
			chunks.put(buf.readLong(), buf.readVarInt());
		}
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.LOADED_CHUNK_VIEW;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());

		buf.writeVarInt(chunks.size());

		for (var entry : chunks.long2IntEntrySet()) {
			buf.writeLong(entry.getLongKey());
			buf.writeVarInt(entry.getIntValue());
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateLoadedChunkView(dimension, chunks);
	}
}