package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author LatvianModder
 */
public class ClientMapDimension
{
	public static ClientMapDimension current;

	public final ClientMapManager manager;
	public final String dimension;
	public final Path directory;
	public final HashMap<XZ, ClientMapRegion> regions;
	public final List<Waypoint> waypoints;

	public ClientMapDimension(ClientMapManager m, String id)
	{
		manager = m;
		dimension = id;
		directory = manager.directory.resolve(dimension.replace(':', '_'));
		regions = new HashMap<>();
		waypoints = new ArrayList<>();
	}

	public ClientMapRegion getRegion(XZ pos)
	{
		return regions.computeIfAbsent(pos, p -> new ClientMapRegion(this, p).load());
	}

	public void release()
	{
		for (ClientMapRegion region : regions.values())
		{
			region.release();
		}

		regions.clear();
		waypoints.clear();
	}

	public ClientMapChunk getChunk(XZ pos)
	{
		return getRegion(XZ.regionFromChunk(pos.x, pos.z)).getChunk(pos);
	}
}