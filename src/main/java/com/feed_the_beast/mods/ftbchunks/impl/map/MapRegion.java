package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class MapRegion implements Runnable
{
	public final MapDimension dimension;
	public final XZ pos;
	public final Map<XZ, MapChunk> chunks;
	public boolean save;
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
		if (!dimension.directory.exists() || !dimension.directory.isDirectory())
		{
			return this;
		}

		File file = new File(dimension.directory, pos.x + "," + pos.z + ",map.png");

		if (!file.exists() || !file.isFile())
		{
			return this;
		}

		try (FileInputStream is = new FileInputStream(file))
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

	public MapChunk getChunk(XZ pos)
	{
		if (pos.x != (pos.x & 31) || pos.z != (pos.z & 31))
		{
			pos = XZ.of(pos.x & 31, pos.z & 31);
		}

		return chunks.computeIfAbsent(pos, p -> new MapChunk(this, p));
	}

	@Override
	public void run()
	{
		if (!save)
		{
			return;
		}

		save = false;

		if (!dimension.directory.exists())
		{
			dimension.directory.mkdirs();
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
				return;
			}
		}

		try (FileOutputStream out = new FileOutputStream(new File(dimension.directory, pos.x + "," + pos.z + ",map.png")))
		{
			ImageIO.write(mapImg, "PNG", out);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		FTBChunks.LOGGER.debug("Saved region " + pos + " - " + chunks.size() + " chunks");
	}
}