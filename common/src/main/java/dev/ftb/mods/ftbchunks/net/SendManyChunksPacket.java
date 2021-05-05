package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
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
public class SendManyChunksPacket extends BasePacket {
	public ResourceKey<Level> dimension;
	public UUID teamId;
	public List<SendChunkPacket.SingleChunk> chunks;

	public SendManyChunksPacket() {
	}

	@Override
	public PacketID getId() {
		return FTBChunksNet.SEND_ALL_CHUNKS;
	}

	SendManyChunksPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		teamId = buf.readUUID();

		int s = buf.readVarInt();
		chunks = new ArrayList<>(s);

		for (int i = 0; i < s; i++) {
			chunks.add(new SendChunkPacket.SingleChunk(buf, teamId));
		}
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeUUID(teamId);
		buf.writeVarInt(chunks.size());

		for (SendChunkPacket.SingleChunk chunk : chunks) {
			chunk.write(buf, teamId);
		}
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateAllChunks(this);
	}
}