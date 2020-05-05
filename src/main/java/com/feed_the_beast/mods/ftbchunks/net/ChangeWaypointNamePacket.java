package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class ChangeWaypointNamePacket
{
	public final UUID id;
	public final String name;

	public ChangeWaypointNamePacket(UUID w, String c)
	{
		id = w;
		name = c;
	}

	ChangeWaypointNamePacket(PacketBuffer buf)
	{
		id = new UUID(buf.readLong(), buf.readLong());
		name = buf.readString(100);
	}

	void write(PacketBuffer buf)
	{
		buf.writeLong(id.getMostSignificantBits());
		buf.writeLong(id.getLeastSignificantBits());
		buf.writeString(name, 100);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity p = context.get().getSender();
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(p);
			Waypoint w = data.waypoints.get(id);

			if (w != null)
			{
				w.name = name;
				data.save();
				SendWaypointsPacket.send(p);
			}
		});

		context.get().setPacketHandled(true);
	}
}