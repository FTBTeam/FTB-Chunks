package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;

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
	public final DimensionType dimension;
	public final Path directory;
	public final HashMap<XZ, ClientMapRegion> regions;
	public final List<Waypoint> waypoints;

	public ClientMapDimension(ClientMapManager m, DimensionType d)
	{
		manager = m;
		dimension = d;
		ResourceLocation id = DimensionType.getKey(dimension);
		directory = manager.directory.resolve(id.getNamespace() + "_" + id.getPath());
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