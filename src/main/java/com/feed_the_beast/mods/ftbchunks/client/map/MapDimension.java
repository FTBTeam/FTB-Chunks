package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import net.minecraft.client.Minecraft;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author LatvianModder
 */
public class MapDimension implements MapTask
{
	private static MapDimension current;

	public static MapDimension getCurrent()
	{
		if (current == null)
		{
			current = MapManager.inst.getDimension(ChunkDimPos.getID(Minecraft.getInstance().world));
		}

		return current;
	}

	public static void updateCurrent()
	{
		current = null;
	}

	public final MapManager manager;
	public final String dimension;
	public final Path directory;
	private Map<XZ, MapRegion> regions;
	public final List<Waypoint> waypoints;
	public boolean saveData;

	public MapDimension(MapManager m, String id)
	{
		manager = m;
		dimension = id;
		directory = manager.directory.resolve(dimension.replace(':', '_'));
		waypoints = new ArrayList<>();
		saveData = false;
	}

	public Collection<MapRegion> getLoadedRegions()
	{
		return regions == null ? Collections.emptyList() : regions.values();
	}

	public Map<XZ, MapRegion> getRegions()
	{
		if (regions == null)
		{
			regions = new HashMap<>();

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

			Path regFile = directory.resolve("dimension.regions");

			if (Files.exists(regFile))
			{
				try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(regFile)))))
				{
					stream.readByte();
					int version = stream.readByte();
					int s = stream.readShort();

					for (int i = 0; i < s; i++)
					{
						int x = stream.readByte();
						int z = stream.readByte();

						MapRegion c = new MapRegion(this, XZ.of(x, z));
						regions.put(c.pos, c);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
			else
			{
				saveData = true;
			}
		}

		return regions;
	}

	public MapRegion getRegion(XZ pos)
	{
		return getRegions().computeIfAbsent(pos, p -> new MapRegion(this, p).created());
	}

	public MapDimension created()
	{
		manager.saveData = true;
		return this;
	}

	public void release()
	{
		for (MapRegion region : getLoadedRegions())
		{
			region.release();
		}

		regions = null;
		waypoints.clear();
	}

	public MapChunk getChunk(XZ pos)
	{
		return getRegion(XZ.regionFromChunk(pos.x, pos.z)).getChunk(pos);
	}

	@Override
	public void runMapTask()
	{
		try
		{
			Collection<MapRegion> regionList = getRegions().values();

			if (regionList.isEmpty())
			{
				return;
			}

			try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(directory.resolve("dimension.regions"))))))
			{
				stream.writeByte(0);
				stream.writeByte(1);
				stream.writeShort(regionList.size());

				for (MapRegion region : regionList)
				{
					stream.writeByte(region.pos.x);
					stream.writeByte(region.pos.z);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void sync()
	{
		long now = System.currentTimeMillis();

		for (MapRegion region : getRegions().values())
		{
			FTBChunksClient.queue(new SyncTXTask(region, now));
		}
	}
}