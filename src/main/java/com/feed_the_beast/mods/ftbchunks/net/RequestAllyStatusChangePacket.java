package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class RequestAllyStatusChangePacket
{
	private final UUID uuid;

	public RequestAllyStatusChangePacket(UUID id)
	{
		uuid = id;
	}

	RequestAllyStatusChangePacket(PacketBuffer buf)
	{
		uuid = new UUID(buf.readLong(), buf.readLong());
	}

	void write(PacketBuffer buf)
	{
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(context.get().getSender());

			if (data.allies.contains(uuid))
			{
				data.allies.remove(uuid);
			}
			else
			{
				data.allies.add(uuid);
			}

			data.save();
		});

		context.get().setPacketHandled(true);
	}
}