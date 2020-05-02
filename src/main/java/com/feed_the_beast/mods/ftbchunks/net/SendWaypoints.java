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
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendWaypoints
{
	public static void send(ServerPlayerEntity player)
	{
		ClaimedChunkPlayerDataImpl data = FTBChunksAPIImpl.manager.getData(player);
		List<Waypoint> waypoints = new ArrayList<>(data.waypoints);
		// TODO: Dynamic waypoints like spawn, homes, etc.

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), new SendWaypoints(waypoints));
	}

	public List<Waypoint> waypoints;

	public SendWaypoints(List<Waypoint> w)
	{
		waypoints = w;
	}

	SendWaypoints(PacketBuffer buf)
	{
		int s = buf.readVarInt();
		waypoints = new ArrayList<>(s);

		for (int i = 0; i < s; i++)
		{
			Waypoint w = new Waypoint();
			w.name = buf.readString(100);
			w.dimension = DimensionType.byName(buf.readResourceLocation());
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
			buf.writeString(w.name, 100);
			buf.writeResourceLocation(DimensionType.getKey(w.dimension));
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