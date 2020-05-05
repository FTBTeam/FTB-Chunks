package com.feed_the_beast.mods.ftbchunks.net;

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
public class DeleteWaypointPacket
{
	public final UUID id;

	public DeleteWaypointPacket(UUID w)
	{
		id = w;
	}

	DeleteWaypointPacket(PacketBuffer buf)
	{
		id = new UUID(buf.readLong(), buf.readLong());
	}

	void write(PacketBuffer buf)
	{
		buf.writeLong(id.getMostSignificantBits());
		buf.writeLong(id.getLeastSignificantBits());
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> {
			ServerPlayerEntity p = context.get().getSender();
			ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(p);
			data.waypoints.remove(id);
			data.save();
			SendWaypointsPacket.send(p);
		});
		context.get().setPacketHandled(true);
	}
}