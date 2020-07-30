package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.server.ServerWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class PregenMapRegionTask implements MapTask, ReloadChunkTask.Callback
{
	public final ServerWorld world;

	public PregenMapRegionTask(ServerWorld w)
	{
		world = w;
	}

	@Override
	public void run()
	{
		try
		{
			File regionFolder = new File(world.getDimension().getType().getDirectory(world.getSaveHandler().getWorldDirectory()), "region");

			if (!regionFolder.exists() || !regionFolder.isDirectory())
			{
				world.getServer().getPlayerList().sendMessage(new StringTextComponent("Map pre-generation is done!"));
				return;
			}

			MapDimension dimension = FTBChunksAPIImpl.manager.map.getDimension(world);
			List<ChunkPos> chunks = new ArrayList<>();

			for (File file : regionFolder.listFiles())
			{
				if (file.isFile())
				{
					String[] s = file.getName().split("\\.");

					if (s.length == 4 && s[0].equals("r") && s[3].equals("mca"))
					{
						try
						{
							MapRegion region = dimension.getRegion(XZ.of(Integer.parseInt(s[1]), Integer.parseInt(s[2])));

							RegionFile regionFile = new RegionFile(file, regionFolder);

							for (int z = 0; z < 32; z++)
							{
								for (int x = 0; x < 32; x++)
								{
									ChunkPos cpos = new ChunkPos((region.pos.x << 5) + x, (region.pos.z << 5) + z);

									if (regionFile.contains(cpos))
									{
										chunks.add(cpos);
									}
								}
							}
						}
						catch (Exception ex)
						{
							ex.printStackTrace();
						}
					}
				}
			}

			for (ChunkPos pos : chunks)
			{
				new ReloadChunkTask(world, XZ.of(pos), this).run();
			}

			//FTBChunksAPIImpl.manager.map.queue(() -> world.getServer().getPlayerList().sendMessage(new StringTextComponent("Region " + (index + 1) + " / " + region.dimension.regions.size() + " generated!")));
			world.getServer().getPlayerList().sendMessage(new StringTextComponent("Map pre-generation is done!"));
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

	@Override
	public void accept(ReloadChunkTask task, boolean changed)
	{
	}
}