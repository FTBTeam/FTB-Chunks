package com.feed_the_beast.mods.ftbchunks.client.map;

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
			/*
			MapDimension dimension = MapManager.inst.getDimension(key.dim);
			MapRegion region = dimension.getRegion(XZ.of(key.x, key.z));

			byte[] dataImgBytes = new byte[stream.readInt()];
			stream.read(dataImgBytes);
			NativeImage dataImg = NativeImage.read(new BufferedInputStream(new ByteArrayInputStream(dataImgBytes)));

			byte[] blockImgBytes = new byte[stream.readInt()];
			stream.read(blockImgBytes);
			NativeImage blockImg = NativeImage.read(new BufferedInputStream(new ByteArrayInputStream(blockImgBytes)));

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
							int ax = chunk.pos.x * 16 + x;
							int az = chunk.pos.z * 16 + z;

							int dc = dataImg.getPixelRGBA(ax, az);
							int bc = blockImg.getPixelRGBA(ax, az);

							if (dc != images.data.getPixelRGBA(ax, az))
							{
								images.data.setPixelRGBA(ax, az, dc);
								changed = true;
							}

							if (bc != images.blocks.getPixelRGBA(ax, az))
							{
								images.blocks.setPixelRGBA(ax, az, bc);
								changed = true;
							}
						}
					}
				}
			}

			if (changed)
			{
				region.update(true);
			}
			 */
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