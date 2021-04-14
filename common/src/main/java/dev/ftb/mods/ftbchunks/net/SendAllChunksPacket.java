package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class SendAllChunksPacket extends MessageBase {
	public ResourceKey<Level> dimension;
	public UUID owner;
	public List<SendChunkPacket.SingleChunk> chunks;

	public SendAllChunksPacket() {
	}

	SendAllChunksPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		owner = new UUID(buf.readLong(), buf.readLong());

		int s = buf.readVarInt();
		chunks = new ArrayList<>(s);

		for (int i = 0; i < s; i++) {
			SendChunkPacket.SingleChunk chunk = new SendChunkPacket.SingleChunk(buf);
			chunk.ownerId = owner;
			chunks.add(chunk);
		}
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeLong(owner.getMostSignificantBits());
		buf.writeLong(owner.getLeastSignificantBits());
		buf.writeVarInt(chunks.size());

		for (SendChunkPacket.SingleChunk chunk : chunks) {
			chunk.write(buf);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateAllChunks(this);
	}
}