package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class MapRegion
{
	public final MapDimension dimension;
	public final XZ pos;
	public final Map<XZ, MapChunk> chunks;
	private boolean save;
	public long lastAccess = 0L;
	private BufferedImage image;

	MapRegion(MapDimension d, XZ p)
	{
		dimension = d;
		pos = p;
		chunks = new HashMap<>();
		save = false;
	}

	public MapRegion load()
	{
		BufferedImage image = getImage();

		for (int cz = 0; cz < 32; cz++)
		{
			for (int cx = 0; cx < 32; cx++)
			{
				if (image.getRGB(cx * 16, cz * 16) != 0)
				{
					MapChunk chunk = new MapChunk(this, XZ.of(cx, cz));
					chunks.put(chunk.pos, chunk);
				}
			}
		}

		FTBChunks.LOGGER.debug("Loaded region " + pos + " - " + chunks.size() + " chunks");
		return this;
	}

	public BufferedImage getImage()
	{
		if (image == null)
		{
			Path file = dimension.directory.resolve(pos.x + "," + pos.z + ",map.png");

			if (Files.exists(file))
			{
				try (InputStream is = Files.newInputStream(file))
				{
					image = ImageIO.read(is);

					if (image.getWidth() != 512 || image.getHeight() != 512)
					{
						image = null;
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}

			if (image == null)
			{
				image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
			}
		}

		return image;
	}

	public MapRegion access()
	{
		lastAccess = System.currentTimeMillis();
		return this;
	}

	public MapChunk getChunk(XZ chunkPos)
	{
		if (chunkPos.x != (chunkPos.x & 31) || chunkPos.z != (chunkPos.z & 31))
		{
			chunkPos = XZ.of(chunkPos.x & 31, chunkPos.z & 31);
		}

		return chunks.computeIfAbsent(chunkPos, p -> new MapChunk(this, p));
	}

	public void save()
	{
		save = true;
	}

	public boolean saveNow()
	{
		for (MapChunk chunk : chunks.values())
		{
			if (chunk.weakUpdate)
			{
				dimension.manager.queueSend(dimension.manager.manager.server.getWorld(dimension.dimension), chunk.getActualPos(), serverPlayerEntity -> true);
				chunk.weakUpdate = false;
			}
		}

		if (!save)
		{
			return true;
		}

		save = false;

		if (Files.notExists(dimension.directory))
		{
			try
			{
				Files.createDirectories(dimension.directory);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		try (OutputStream out = Files.newOutputStream(dimension.directory.resolve(pos.x + "," + pos.z + ",map.png")))
		{
			ImageIO.write(getImage(), "PNG", out);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		FTBChunks.LOGGER.debug("Saved region " + pos + " - " + chunks.size() + " chunks");
		return false;
	}
}