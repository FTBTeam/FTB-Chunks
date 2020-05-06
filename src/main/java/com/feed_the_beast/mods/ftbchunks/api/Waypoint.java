package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.world.dimension.DimensionType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class Waypoint
{
	public final ClaimedChunkPlayerData playerData;
	public final UUID id;
	public String name = "";
	public String owner = "";
	public int x = 0;
	public int y = 0;
	public int z = 0;
	public DimensionType dimension = DimensionType.OVERWORLD;
	public PrivacyMode privacy = PrivacyMode.PRIVATE;
	public int color = 0xFFFFFF;
	public WaypointType type = WaypointType.DEFAULT;

	public Waypoint(@Nullable ClaimedChunkPlayerData d, UUID i)
	{
		playerData = d;
		id = i;
	}

	public Waypoint copy()
	{
		Waypoint w = new Waypoint(null, id);
		w.name = name;
		w.x = x;
		w.y = y;
		w.z = z;
		w.dimension = dimension;
		w.privacy = privacy;
		w.color = color;
		w.type = type;
		return w;
	}
}