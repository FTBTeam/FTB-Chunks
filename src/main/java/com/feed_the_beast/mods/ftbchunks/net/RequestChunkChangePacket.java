package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.api.ClaimResult;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkPlayerData;
import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
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
	private final int action;
	private final Set<XZ> chunks;

	public RequestChunkChangePacket(int a, Set<XZ> c)
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
			chunks.add(XZ.of(x, z));
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(action);
		buf.writeVarInt(chunks.size());

		for (XZ pos : chunks)
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
			Consumer<XZ> consumer;
			Instant time = Instant.now();

			switch (action)
			{
				case 0:
					consumer = pos -> {
						ClaimResult result = data.claim(source, pos.dim(player.world), false);

						if (result.isSuccess())
						{
							result.setClaimedTime(time);
						}
					};
					break;
				case 1:
					consumer = pos -> data.unclaim(source, pos.dim(player.world), false);
					break;
				case 2:
					consumer = pos -> {
						ClaimResult result = data.load(source, pos.dim(player.world), false);

						if (result.isSuccess())
						{
							result.setForceLoadedTime(time);
						}
					};
					break;
				case 3:
					consumer = pos -> data.unload(source, pos.dim(player.world), false);
					break;
				default:
					FTBChunks.LOGGER.warn("Unknown chunk action ID: " + action);
					return;
			}

			for (XZ pos : chunks)
			{
				consumer.accept(pos);
				//FIXME: FTBChunksAPIImpl.manager.map.queueSend(player.world, pos, p -> p == player);
			}

			SendGeneralDataPacket.send(data, player);
		});

		context.get().setPacketHandled(true);
	}
}