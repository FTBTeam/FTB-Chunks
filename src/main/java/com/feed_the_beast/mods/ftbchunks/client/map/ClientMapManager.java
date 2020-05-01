package com.feed_the_beast.mods.ftbchunks.client.map;

import net.minecraft.world.dimension.DimensionType;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClientMapManager
{
	public static ClientMapManager inst;

	public final UUID serverId;
	public final Path directory;
	public final Map<DimensionType, ClientMapDimension> dimensions;

	public ClientMapManager(UUID id, Path dir)
	{
		serverId = id;
		directory = dir;
		dimensions = new HashMap<>();
	}

	public ClientMapDimension getDimension(DimensionType dim)
	{
		return dimensions.computeIfAbsent(dim, d -> new ClientMapDimension(this, d));
	}

	public void release()
	{
		for (ClientMapDimension dimension : dimensions.values())
		{
			dimension.release();
		}

		dimensions.clear();
	}
}