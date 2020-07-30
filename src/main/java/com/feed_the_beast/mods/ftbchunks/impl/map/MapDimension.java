package com.feed_the_beast.mods.ftbchunks.impl.map;

import java.nio.file.Path;
import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class MapDimension
{
	public final MapManager manager;
	public final String dimension;
	public final Path directory;
	public final HashMap<XZ, MapRegion> regions;

	MapDimension(MapManager m, String d)
	{
		manager = m;
		dimension = d;
		directory = manager.manager.dataDirectory.resolve("map/" + dimension.replace(':', '_'));
		regions = new HashMap<>();
	}

	public MapRegion getRegion(XZ pos)
	{
		return regions.computeIfAbsent(pos, p -> new MapRegion(this, p).load());
	}
}