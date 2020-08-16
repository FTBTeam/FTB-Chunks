package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import net.minecraft.client.renderer.texture.NativeImage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author LatvianModder
 */
public class SyncRXTask implements MapTask
{
	public final RegionSyncKey key;
	public final byte[] data;
	public final long now;

	public SyncRXTask(RegionSyncKey k, byte[] d)
	{
		key = k;
		data = d;
		now = System.currentTimeMillis();
	}

	@Override
	public void runMapTask()
	{
		ByteArrayInputStream in = new ByteArrayInputStream(data);

		try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new InflaterInputStream(in))))
		{
			MapDimension dimension = MapManager.inst.getDimension(key.dim);
			MapRegion region = dimension.getRegion(XZ.of(key.x, key.z));

			byte[] imgData = new byte[stream.readInt()];
			stream.read(imgData);
			NativeImage image = NativeImage.read(new BufferedInputStream(new ByteArrayInputStream(imgData)));
			boolean changed = false;

			int s = stream.readShort();

			for (int i = 0; i < s; i++)
			{
				int cx = stream.readByte() & 0xFF;
				int cz = stream.readByte() & 0xFF;
				long mod = now - stream.readLong();
				MapChunk chunk = region.getChunk(XZ.of(cx, cz));

				if (mod > chunk.modified)
				{
					chunk.modified = mod;

					for (int z = 0; z < 16; z++)
					{
						for (int x = 0; x < 16; x++)
						{
							int c = image.getPixelRGBA(chunk.pos.x * 16 + x + 1, chunk.pos.z * 16 + z + 1);
							int a = NativeImage.getAlpha(c);
							int r = NativeImage.getRed(c);
							int g = NativeImage.getGreen(c);
							int b = NativeImage.getBlue(c);
							changed = chunk.setHRGB(x, z, (a << 24) | (r << 16) | (g << 8) | b) | changed;
						}
					}
				}
			}

			if (changed)
			{
				region.update(true);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public boolean cancelOtherTasks()
	{
		return true;
	}
}