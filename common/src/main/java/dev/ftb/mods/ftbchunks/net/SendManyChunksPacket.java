package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket.SingleChunk;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class SendManyChunksPacket extends BaseS2CMessage {
	public final ResourceKey<Level> dimension;
	public final UUID teamId;
	public final List<SingleChunk> chunks;

	public SendManyChunksPacket(ResourceKey<Level> dimension, UUID teamId, List<SingleChunk> chunks) {
		this.dimension = dimension;
		this.teamId = teamId;
		this.chunks = chunks;
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_ALL_CHUNKS;
	}

	SendManyChunksPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
		teamId = buf.readUUID();
		chunks = buf.readList(buf1 -> new SingleChunk(buf1, teamId));
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeUUID(teamId);
		buf.writeCollection(chunks, (buf1, chunk) -> chunk.write(buf1, teamId));
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunksClient.INSTANCE.updateChunksFromServer(dimension, teamId, chunks);
	}
}