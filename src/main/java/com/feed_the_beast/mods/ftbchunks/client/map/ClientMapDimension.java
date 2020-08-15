package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
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
public class ClientMapDimension implements MapTask
{
	private static ClientMapDimension current;

	public static ClientMapDimension getCurrent()
	{
		if (current == null)
		{
			current = ClientMapManager.inst.getDimension(ChunkDimPos.getID(Minecraft.getInstance().world));
		}

		return current;
	}

	public static void updateCurrent()
	{
		current = null;
	}

	public final ClientMapManager manager;
	public final String dimension;
	public final Path directory;
	private Map<XZ, ClientMapRegion> regions;
	public final List<Waypoint> waypoints;
	public boolean saveData;

	public ClientMapDimension(ClientMapManager m, String id)
	{
		manager = m;
		dimension = id;
		directory = manager.directory.resolve(dimension.replace(':', '_'));
		waypoints = new ArrayList<>();
		saveData = false;
	}

	public Collection<ClientMapRegion> getLoadedRegions()
	{
		return regions == null ? Collections.emptyList() : regions.values();
	}

	public Map<XZ, ClientMapRegion> getRegions()
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

			try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(directory.resolve("dimension.regions"))))))
			{
				stream.readByte();
				int version = stream.readByte();
				int s = stream.readShort();

				for (int i = 0; i < s; i++)
				{
					int x = stream.readByte();
					int z = stream.readByte();

					ClientMapRegion c = new ClientMapRegion(this, XZ.of(x, z));
					regions.put(c.pos, c);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		return regions;
	}

	public ClientMapRegion getRegion(XZ pos)
	{
		return getRegions().computeIfAbsent(pos, p -> new ClientMapRegion(this, p).created());
	}

	public ClientMapDimension created()
	{
		manager.saveData = true;
		return this;
	}

	public void release()
	{
		for (ClientMapRegion region : getLoadedRegions())
		{
			region.release();
		}

		regions = null;
		waypoints.clear();
	}

	public ClientMapChunk getChunk(XZ pos)
	{
		return getRegion(XZ.regionFromChunk(pos.x, pos.z)).getChunk(pos);
	}

	@Override
	public void runMapTask()
	{
		try
		{
			Collection<ClientMapRegion> regionList = getRegions().values();

			if (regionList.isEmpty())
			{
				return;
			}

			try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(directory.resolve("dimension.regions"))))))
			{
				stream.writeByte(0);
				stream.writeByte(1);
				stream.writeShort(regionList.size());

				for (ClientMapRegion region : regionList)
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
}