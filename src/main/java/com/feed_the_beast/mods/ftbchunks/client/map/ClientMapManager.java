package com.feed_the_beast.mods.ftbchunks.client.map;

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
public class ClientMapManager implements MapTask
{
	public static ClientMapManager inst;

	public final UUID serverId;
	public final Path directory;
	private Map<String, ClientMapDimension> dimensions;
	public boolean saveData;

	public ClientMapManager(UUID id, Path dir)
	{
		serverId = id;
		directory = dir;
		saveData = false;
	}

	public Collection<ClientMapDimension> getLoadedDimensions()
	{
		return dimensions == null ? Collections.emptyList() : dimensions.values();
	}

	public Map<String, ClientMapDimension> getDimensions()
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
							dimensions.put(s, new ClientMapDimension(this, s));
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

	public ClientMapDimension getDimension(String dim)
	{
		return dimensions.computeIfAbsent(dim, d -> new ClientMapDimension(this, d).created());
	}

	public void release()
	{
		for (ClientMapDimension dimension : getLoadedDimensions())
		{
			dimension.release();
		}

		dimensions = null;
	}

	public void updateAllRegions(boolean save)
	{
		for (ClientMapDimension dimension : getDimensions().values())
		{
			for (ClientMapRegion region : dimension.getRegions().values())
			{
				region.update(save);
			}
		}
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