package com.feed_the_beast.mods.ftbchunks.impl.map;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class MapDimension
{
	public final MapManager manager;
	public final DimensionType dimension;
	public final Path directory;
	public final HashMap<XZ, MapRegion> regions;

	MapDimension(MapManager m, DimensionType d)
	{
		manager = m;
		dimension = d;
		ResourceLocation id = DimensionType.getKey(dimension);
		directory = manager.manager.dataDirectory.resolve("map/" + id.getNamespace() + "_" + id.getPath());
		regions = new HashMap<>();
	}

	public ServerWorld getWorld()
	{
		return manager.manager.server.getWorld(dimension);
	}

	public MapRegion getRegion(XZ pos)
	{
		return regions.computeIfAbsent(pos, p -> new MapRegion(this, p).load());
	}
}