package com.feed_the_beast.mods.ftbchunks.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class MinimapDimension
{
	public final MinimapData data;
	public final DimensionType type;

	public final List<Waypoint> waypoints;
	public final Long2ObjectOpenHashMap<MinimapArea> areas;

	public MinimapDimension(MinimapData d, DimensionType t)
	{
		data = d;
		type = t;

		waypoints = new ArrayList<>();
		areas = new Long2ObjectOpenHashMap<>();
	}

	public MinimapArea getArea(ChunkPos pos)
	{
		return areas.computeIfAbsent(pos.asLong(), p -> new MinimapArea(this, new ChunkPos(p)));
	}
}