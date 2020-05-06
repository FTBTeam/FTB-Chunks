package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.api.PrivacyMode;
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
public class ChangeWaypointPrivacyPacket
{
	public final UUID id;
	public final int privacy;

	public ChangeWaypointPrivacyPacket(UUID w, int c)
	{
		id = w;
		privacy = c;
	}

	ChangeWaypointPrivacyPacket(PacketBuffer buf)
	{
		id = new UUID(buf.readLong(), buf.readLong());
		privacy = buf.readInt();
	}

	void write(PacketBuffer buf)
	{
		buf.writeLong(id.getMostSignificantBits());
		buf.writeLong(id.getLeastSignificantBits());
		buf.writeInt(privacy);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity p = context.get().getSender();
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(p);
			Waypoint w = data.waypoints.get(id);

			if (w != null)
			{
				w.privacy = PrivacyMode.values()[privacy];
				data.save();
				SendWaypointsPacket.send(p);
			}
		});
		context.get().setPacketHandled(true);
	}
}