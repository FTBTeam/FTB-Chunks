package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class MapManager implements MapTask
{
	public static MapManager inst;

	public final UUID serverId;
	public final Path directory;
	private Map<String, MapDimension> dimensions;
	public boolean saveData;

	public MapManager(UUID id, Path dir)
	{
		serverId = id;
		directory = dir;
		saveData = false;
	}

	public Collection<MapDimension> getLoadedDimensions()
	{
		return dimensions == null ? Collections.emptyList() : dimensions.values();
	}

	public Map<String, MapDimension> getDimensions()
	{
		if (dimensions == null)
		{
			dimensions = new HashMap<>();

			try
			{
				if (Files.notExists(directory))
				{
					try
					{
						Files.createDirectories(directory);
					}
					catch (Exception ex)
					{
						throw new RuntimeException(ex);
					}
				}

				Path file = directory.resolve("dimensions.txt");

				if (Files.exists(file))
				{
					for (String s : Files.readAllLines(file))
					{
						s = s.trim();

						if (s.length() >= 3)
						{
							dimensions.put(s, new MapDimension(this, s));
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

	public MapDimension getDimension(String dim)
	{
		return dimensions.computeIfAbsent(dim, d -> new MapDimension(this, d).created());
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
			Files.write(directory.resolve("dimensions.txt"), dimensions.keySet());
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}