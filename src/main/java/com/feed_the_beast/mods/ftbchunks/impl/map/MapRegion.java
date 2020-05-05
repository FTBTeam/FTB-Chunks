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

	MapRegion(MapDimension d, XZ p)
	{
		dimension = d;
		pos = p;
		chunks = new HashMap<>();
		save = false;
	}

	public MapRegion load()
	{
		Path file = dimension.directory.resolve(pos.x + "," + pos.z + ",map.png");

		if (Files.notExists(file))
		{
			return this;
		}

		try (InputStream is = Files.newInputStream(file))
		{
			BufferedImage image = ImageIO.read(is);

			if (image.getWidth() == 512 && image.getHeight() == 512)
			{
				for (int cz = 0; cz < 32; cz++)
				{
					for (int cx = 0; cx < 32; cx++)
					{
						if (image.getRGB(cx * 16, cz * 16) != 0)
						{
							MapChunk chunk = new MapChunk(this, XZ.of(cx, cz));

							for (int z = 0; z < 16; z++)
							{
								for (int x = 0; x < 16; x++)
								{
									int i = x + z * 16;
									int c = image.getRGB(cx * 16 + x, cz * 16 + z);
									chunk.height[i] = (byte) (c >> 24);
									chunk.red[i] = (byte) (c >> 16);
									chunk.green[i] = (byte) (c >> 8);
									chunk.blue[i] = (byte) (c >> 0);
								}
							}

							chunks.put(chunk.pos, chunk);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		FTBChunks.LOGGER.debug("Loaded region " + pos + " - " + chunks.size() + " chunks");
		return this;
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

		BufferedImage mapImg = new BufferedImage(16 * 32, 16 * 32, BufferedImage.TYPE_INT_ARGB);

		for (MapChunk c : chunks.values())
		{
			try
			{
				for (int i = 0; i < 256; i++)
				{
					int h = c.height[i] & 0xFF;
					int r = c.red[i] & 0xFF;
					int g = c.green[i] & 0xFF;
					int b = c.blue[i] & 0xFF;

					int x = c.pos.x * 16 + (i % 16);
					int y = c.pos.z * 16 + (i / 16);
					int col = (h << 24) | (r << 16) | (g << 8) | b;

					mapImg.setRGB(x, y, col);
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				FTBChunks.LOGGER.info("Failed to save chunk " + c.pos + " in " + pos + ": " + ex);
				return false;
			}
		}

		try (OutputStream out = Files.newOutputStream(dimension.directory.resolve(pos.x + "," + pos.z + ",map.png")))
		{
			ImageIO.write(mapImg, "PNG", out);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		FTBChunks.LOGGER.debug("Saved region " + pos + " - " + chunks.size() + " chunks");
		return false;
	}
}