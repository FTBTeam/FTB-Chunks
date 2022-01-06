package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author LatvianModder
 */
public class LoadedChunkViewPacket extends BaseS2CMessage {
	public final ResourceKey<Level> dimension;
	public final Collection<ChunkPos> chunks;

	public LoadedChunkViewPacket(ResourceKey<Level> d, Collection<ChunkPos> c) {
		dimension = d;
		chunks = c;
	}

	LoadedChunkViewPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		int s = buf.readVarInt();

		chunks = new ArrayList<>(s);

		for (int i = 0; i < s; i++) {
			chunks.add(new ChunkPos(buf.readVarInt(), buf.readVarInt()));
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

		for (ChunkPos pos : chunks) {
			buf.writeVarInt(pos.x);
			buf.writeVarInt(pos.z);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateLoadedChunkView(dimension, chunks);
	}
}