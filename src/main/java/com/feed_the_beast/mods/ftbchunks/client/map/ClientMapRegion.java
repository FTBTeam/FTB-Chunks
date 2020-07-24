package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.map.MapTask;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import net.minecraft.client.renderer.texture.NativeImage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ClientMapRegion implements MapTask
{
	private static final int NO_CHUNK = NativeImage.getCombined(255, 0, 0, 0);

	public final ClientMapDimension dimension;
	public final XZ pos;
	public final Map<XZ, ClientMapChunk> chunks;
	private NativeImage image;
	public boolean saveImage;

	public ClientMapRegion(ClientMapDimension d, XZ p)
	{
		dimension = d;
		pos = p;
		chunks = new HashMap<>();
		image = null;
		saveImage = false;
	}

	public ClientMapRegion load()
	{
		if (Files.notExists(dimension.directory))
		{
			return this;
		}

		Path file = dimension.directory.resolve(pos.x + "," + pos.z + ",map.png");

		if (Files.notExists(file))
		{
			return this;
		}

		try (InputStream is = Files.newInputStream(file))
		{
			image = NativeImage.read(is);

			if (image.getWidth() == 512 && image.getHeight() == 512)
			{
				for (int cz = 0; cz < 32; cz++)
				{
					for (int cx = 0; cx < 32; cx++)
					{
						if (image.getPixelRGBA(cx * 16, cz * 16) != NO_CHUNK)
						{
							ClientMapChunk chunk = new ClientMapChunk(this, XZ.of(cx, cz));
							chunks.put(chunk.pos, chunk);
						}
					}
				}
			}
			else
			{
				image.close();
				image = null;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			image = null;
		}

		// FTBChunks.LOGGER.debug("Loaded client region " + pos + " - " + chunks.size() + " chunks");
		return this;
	}

	public NativeImage getImage()
	{
		if (image == null)
		{
			image = new NativeImage(NativeImage.PixelFormat.RGBA, 512, 512, true);
			image.fillAreaRGBA(0, 0, 512, 512, NO_CHUNK);
			saveImage = true;
		}

		return image;
	}

	public ClientMapChunk getChunk(XZ pos)
	{
		if (pos.x != (pos.x & 31) || pos.z != (pos.z & 31))
		{
			pos = XZ.of(pos.x & 31, pos.z & 31);
		}

		return chunks.computeIfAbsent(pos, p -> new ClientMapChunk(this, p));
	}

	public void release()
	{
		if (image != null)
		{
			image.close();
			image = null;
		}
	}

	@Override
	public void run()
	{
		try
		{
			if (Files.notExists(dimension.directory))
			{
				Files.createDirectories(dimension.directory);
			}

			getImage().write(dimension.directory.resolve(pos.x + "," + pos.z + ",map.png"));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}