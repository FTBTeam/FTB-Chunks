package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.ClaimedChunkPlayerData;
import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResult;
import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RequestChunkChangePacket
{
	private int action;
	private Set<ChunkPos> chunks;

	public RequestChunkChangePacket(int a, Set<ChunkPos> c)
	{
		action = a;
		chunks = c;
	}

	RequestChunkChangePacket(PacketBuffer buf)
	{
		action = buf.readVarInt();
		int s = buf.readVarInt();
		chunks = new LinkedHashSet<>(s);

		for (int i = 0; i < s; i++)
		{
			int x = buf.readVarInt();
			int z = buf.readVarInt();
			chunks.add(new ChunkPos(x, z));
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(action);
		buf.writeVarInt(chunks.size());

		for (ChunkPos pos : chunks)
		{
			buf.writeVarInt(pos.x);
			buf.writeVarInt(pos.z);
		}
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity player = context.get().getSender();
			CommandSource source = player.getCommandSource();
			ClaimedChunkPlayerData data = FTBChunksAPI.INSTANCE.getManager().getData(player);
			Consumer<ChunkPos> consumer;
			Instant time = Instant.now();

			switch (action)
			{
				case 0:
					consumer = pos -> {
						ClaimResult result = data.claim(source, new ChunkDimPos(player.dimension, pos), false);

						if (result.isSuccess())
						{
							result.setClaimedTime(time);
						}
					};
					break;
				case 1:
					consumer = pos -> data.unclaim(source, new ChunkDimPos(player.dimension, pos), false);
					break;
				case 2:
					consumer = pos -> {
						ClaimResult result = data.load(source, new ChunkDimPos(player.dimension, pos), false);

						if (result.isSuccess())
						{
							result.setForceLoadedTime(time);
						}
					};
					break;
				case 3:
					consumer = pos -> data.unload(source, new ChunkDimPos(player.dimension, pos), false);
					break;
				default:
					FTBChunks.LOGGER.warn("Unknown chunk action ID: " + action);
					return;
			}

			chunks.forEach(consumer);
			SendMapDataPacket.send(player);
		});
		context.get().setPacketHandled(true);
	}
}