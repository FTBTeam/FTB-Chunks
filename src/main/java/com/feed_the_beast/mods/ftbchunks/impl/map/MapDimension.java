package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author LatvianModder
 */
public class MapDimension
{
	public final MapImageManager manager;
	public final DimensionType dimension;
	public final File directory;
	public final HashMap<XZ, MapRegion> regions;

	MapDimension(MapImageManager m, DimensionType d)
	{
		manager = m;
		dimension = d;
		ResourceLocation id = DimensionType.getKey(dimension);
		directory = new File(manager.directory, id.getNamespace() + "_" + id.getPath() + "/");
		regions = new HashMap<>();
	}

	public MapRegion getRegion(XZ pos)
	{
		return regions.computeIfAbsent(pos, p -> new MapRegion(this, p).load());
	}

	private int getHeight(MapChunk chunk, int x, int z)
	{
		int hcx = x >> 4;
		int hcz = z >> 4;

		if (hcx == (chunk.pos.x + chunk.region.pos.x * 16) && hcz == (chunk.pos.z + chunk.region.pos.z * 16))
		{
			return chunk.getHeight(x, z);
		}

		return getRegion(XZ.regionFromBlock(x, z)).access().getChunk(XZ.chunkFromBlock(x, z)).getHeight(x, z);
	}

	public void loadAllRegions()
	{
		File[] files = directory.listFiles();

		if (files == null || files.length == 0)
		{
			return;
		}

		for (File f : files)
		{
			if (f.isFile() && f.getName().endsWith(",map.png"))
			{
				String[] s = f.getName().split(",", 3);

				if (s.length == 3)
				{
					try
					{
						int x = Integer.parseInt(s[0]);
						int z = Integer.parseInt(s[1]);
						getRegion(XZ.of(x, z));
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}
		}
	}

	public void exportPng()
	{
		loadAllRegions();

		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;

		List<MapChunk> chunks = new ArrayList<>();

		for (MapRegion region : regions.values())
		{
			for (MapChunk chunk : region.chunks.values())
			{
				int cx = (region.pos.x << 5) + chunk.pos.x;
				int cz = (region.pos.z << 5) + chunk.pos.z;
				minX = Math.min(minX, cx);
				minZ = Math.min(minZ, cz);
				maxX = Math.max(maxX, cx);
				maxZ = Math.max(maxZ, cz);
				chunks.add(chunk);
			}
		}

		if (chunks.isEmpty())
		{
			return;
		}

		FTBChunks.LOGGER.info("Creating " + ((maxX - minX + 1) * 16) + " x " + ((maxZ - minZ + 1) * 16) + " image from " + regions.size() + " regions and " + chunks.size() + " chunks...");
		BufferedImage image = new BufferedImage((maxX - minX + 1) * 16, (maxZ - minZ + 1) * 16, BufferedImage.TYPE_INT_RGB);
		BufferedImage imageSmall = new BufferedImage(maxX - minX + 1, maxZ - minZ + 1, BufferedImage.TYPE_INT_RGB);

		for (MapChunk chunk : chunks)
		{
			int cx = (chunk.region.pos.x << 5) + chunk.pos.x;
			int cz = (chunk.region.pos.z << 5) + chunk.pos.z;
			float ar = 0F;
			float ag = 0F;
			float ab = 0F;

			for (int z = 0; z < 16; z++)
			{
				for (int x = 0; x < 16; x++)
				{
					int by = getHeight(chunk, cx * 16 + x, cz * 16 + z);

					if (by == -1)
					{
						continue;
					}

					int bn = getHeight(chunk, cx * 16 + x, cz * 16 + z - 1);
					int bw = getHeight(chunk, cx * 16 + x - 1, cz * 16 + z);
					float addedBrightness = MathUtils.RAND.nextFloat() * 0.05F;

					if (bn != -1 || bw != -1)
					{
						if (by > bn || by > bw)
						{
							addedBrightness += 0.1F;
						}

						if (by < bn || by < bw)
						{
							addedBrightness -= 0.1F;
						}
					}

					int i = MapChunk.index(x, z);
					int r = chunk.red[i] & 0xFF;
					int g = chunk.green[i] & 0xFF;
					int b = chunk.blue[i] & 0xFF;

					ar += (r / 255F) * (r / 255F);
					ag += (g / 255F) * (g / 255F);
					ab += (b / 255F) * (b / 255F);

					int c = ColorBlend.addBrightness(Color4I.rgb(0xFF000000 | (r << 16) | (g << 8) | b), addedBrightness);
					image.setRGB((cx - minX) * 16 + x, (cz - minZ) * 16 + z, 0xFF000000 | c);
				}
			}

			int air = (int) (Math.sqrt(ar / 256F) * 255F) & 0xFF;
			int aig = (int) (Math.sqrt(ag / 256F) * 255F) & 0xFF;
			int aib = (int) (Math.sqrt(ab / 256F) * 255F) & 0xFF;
			imageSmall.setRGB(cx - minX, cz - minZ, 0xFF000000 | (air << 16) | (aig << 8) | (aib));
		}

		ResourceLocation id = DimensionType.getKey(dimension);

		try (FileOutputStream out = new FileOutputStream(new File(manager.manager.localDirectory, id.getNamespace() + "_" + id.getPath() + "_map.png")))
		{
			ImageIO.write(image, "PNG", out);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		try (FileOutputStream out = new FileOutputStream(new File(manager.manager.localDirectory, id.getNamespace() + "_" + id.getPath() + "_map_small.png")))
		{
			ImageIO.write(imageSmall, "PNG", out);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}