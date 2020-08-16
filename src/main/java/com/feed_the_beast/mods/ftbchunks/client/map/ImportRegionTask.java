package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.RegionFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author LatvianModder
 */
public class ImportRegionTask implements MapTask
{
	public final MapDimension dimension;
	public final File file, dir;
	public final int rx, rz;

	public ImportRegionTask(MapDimension dim, File f, File d, int x, int z)
	{
		dimension = dim;
		file = f;
		dir = d;
		rx = x;
		rz = z;
	}

	@Override
	public void runMapTask()
	{
		FTBChunks.LOGGER.info("Importing region " + rx + ", " + rz);

		try (RegionFile regionFile = new RegionFile(file, dir, true))
		{
			for (int cx = 0; cx < 32; cx++)
			{
				for (int cz = 0; cz < 32; cz++)
				{
					ChunkPos chunkPos = new ChunkPos(cx, cz);

					if (regionFile.contains(chunkPos))
					{
						try (DataInputStream in = regionFile.func_222666_a(chunkPos))
						{
							if (in != null)
							{
								MapChunk chunk = dimension.getChunk(XZ.of(rx * 32 + cx, rz * 32 + cz));
								CompoundNBT nbt = CompressedStreamTools.read(in);

								System.out.println(chunk.pos + " " + nbt.getCompound("Level").keySet());
							}
						}
						catch (IOException ioexception)
						{
						}
					}
				}
			}
		}
		catch (IOException ex)
		{
		}
	}

	@Override
	public boolean cancelOtherTasks()
	{
		return true;
	}
}