package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class MapManager implements MapTask
{
	public static MapManager inst;

	public final UUID serverId;
	public final Path directory;
	private Map<RegistryKey<World>, MapDimension> dimensions;
	public boolean saveData;

	public MapManager(UUID id, Path dir)
	{
		serverId = id;
		directory = dir;
		dimensions = null;
		saveData = false;
	}

	public Collection<MapDimension> getLoadedDimensions()
	{
		return dimensions == null ? Collections.emptyList() : dimensions.values();
	}

	public Map<RegistryKey<World>, MapDimension> getDimensions()
	{
		if (dimensions == null)
		{
			dimensions = new LinkedHashMap<>();

			try
			{
				Path file = directory.resolve("dimensions.txt");

				if (Files.exists(file))
				{
					for (String s : Files.readAllLines(file))
					{
						s = s.trim();

						if (s.length() >= 3)
						{
							RegistryKey<World> key = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(s));
							dimensions.put(key, new MapDimension(this, key));
						}
					}
				}
				else
				{
					saveData = true;
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		return dimensions;
	}

	public MapDimension getDimension(RegistryKey<World> dim)
	{
		return getDimensions().computeIfAbsent(dim, d -> new MapDimension(this, d).created());
	}

	public void release()
	{
		for (MapDimension dimension : getLoadedDimensions())
		{
			dimension.release();
		}

		dimensions = null;
	}

	public void updateAllRegions(boolean save)
	{
		for (MapDimension dimension : getDimensions().values())
		{
			for (MapRegion region : dimension.getRegions().values())
			{
				region.update(save);
			}
		}

		FTBChunksClient.updateMinimap = true;
	}

	@Override
	public void runMapTask()
	{
		try
		{
			Files.write(directory.resolve("dimensions.txt"), dimensions
					.keySet()
					.stream()
					.map(key -> key.getLocation().toString())
					.collect(Collectors.toList())
			);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}