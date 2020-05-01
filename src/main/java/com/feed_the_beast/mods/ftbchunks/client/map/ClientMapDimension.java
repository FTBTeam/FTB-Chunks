package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

	public Path exportHTML() throws Exception
	{
		List<String> lines = new ArrayList<>();
		lines.add("<!DOCTYPE html>");
		lines.add("<html>");
		lines.add("<head>");
		lines.add("<style>");
		lines.add("img{position:absolute;width:512px;height:512px;margin:0;border:0;padding:0;}");
		lines.add("html{background-color:black;}");
		lines.add("</style>");
		lines.add("</head>");
		lines.add("<body>");

		List<XZ> regions = Files.list(directory)
				.map(path -> path.getFileName().toString())
				.filter(name -> name.endsWith(".png"))
				.map(name -> name.split(","))
				.filter(name -> name.length == 3)
				.map(name -> XZ.of(Integer.parseInt(name[0]), Integer.parseInt(name[1])))
				.collect(Collectors.toList());

		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;

		for (XZ pos : regions)
		{
			minX = Math.min(minX, pos.x);
			minZ = Math.min(minZ, pos.z);
		}

		for (XZ pos : regions)
		{
			lines.add("<img title='Region " + pos.x + ":" + pos.z + "' src='" + pos.x + "," + pos.z + ",map.png' style='left:" + ((pos.x - minX) * 512) + "px;top:" + ((pos.z - minZ) * 512) + "px;'>");
		}

		lines.add("</body>");
		lines.add("</html>");

		Path file = directory.resolve("index.html");
		Files.write(file, lines);
		return file;
	}
}