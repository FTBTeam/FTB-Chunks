package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.api.PrivacyMode;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.api.WaypointType;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendWaypointsPacket
{
	public static void send(ServerPlayerEntity player)
	{
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		List<Waypoint> waypoints = new ArrayList<>(data.waypoints.values());
		// TODO: Dynamic waypoints like spawn, homes, etc.

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendWaypointsPacket(waypoints));
	}

	public List<Waypoint> waypoints;

	public SendWaypointsPacket(List<Waypoint> w)
	{
		waypoints = w;
	}

	SendWaypointsPacket(PacketBuffer buf)
	{
		int s = buf.readVarInt();
		waypoints = new ArrayList<>(s);

		for (int i = 0; i < s; i++)
		{
			Waypoint w = new Waypoint();
			w.id = new UUID(buf.readLong(), buf.readLong());
			w.name = buf.readString(100);
			w.dimension = DimensionType.getById(buf.readVarInt());
			w.x = buf.readVarInt();
			w.y = buf.readVarInt();
			w.z = buf.readVarInt();
			w.color = buf.readInt();
			w.mode = PrivacyMode.VALUES[buf.readByte()];
			w.type = WaypointType.VALUES[buf.readByte()];
			waypoints.add(w);
		}
	}

	void write(PacketBuffer buf)
	{
		buf.writeVarInt(waypoints.size());

		for (Waypoint w : waypoints)
		{
			buf.writeLong(w.id.getMostSignificantBits());
			buf.writeLong(w.id.getLeastSignificantBits());
			buf.writeString(w.name, 100);
			buf.writeVarInt(w.dimension.getId());
			buf.writeVarInt(w.x);
			buf.writeVarInt(w.y);
			buf.writeVarInt(w.z);
			buf.writeInt(w.color);
			buf.writeByte(w.mode.ordinal());
			buf.writeByte(w.type.ordinal());
		}
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateWaypoints(this));
		context.get().setPacketHandled(true);
	}
}