package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClientConfig;
import com.feed_the_beast.mods.ftbchunks.client.map.color.ColorUtils;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.BlockState;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;

/**
 * @author LatvianModder
 */
public class ReloadChunkTask implements MapTask
{
	public final World world;
	public final ChunkPos pos;
	private final MapManager manager;

	public ReloadChunkTask(World w, ChunkPos p)
	{
		world = w;
		pos = p;
		manager = MapManager.inst;
	}

	@Override
	public void runMapTask()
	{
		if (MapManager.inst != manager)
		{
			return;
		}

		RegistryKey<World> dimId = world.getDimensionKey();

		IChunk ichunk = world.getChunk(pos.x, pos.z, ChunkStatus.FULL, false);

		if (ichunk == null)
		{
			return;
		}

		MapChunk mapChunk = manager.getDimension(dimId).getRegion(XZ.regionFromChunk(pos)).getChunk(XZ.of(pos));

		int topY = world.func_234938_ad_() + 1;
		BlockPos.Mutable blockPos = new BlockPos.Mutable();
		int blockX = pos.getXStart();
		int blockZ = pos.getZStart();

		boolean changed = false;
		boolean[] flags = new boolean[1];

		try
		{
			for (int wi = 0; wi < 256; wi++)
			{
				int wx = wi % 16;
				int wz = wi / 16;
				blockPos.setPos(blockX + wx, topY, blockZ + wz);
				int by = MathHelper.clamp(MapChunk.setHeight(ichunk, blockPos, flags).getY(), 1, 255);
				blockPos.setY(by);
				BlockState state = ichunk.getBlockState(blockPos);
				Color4I color = ColorUtils.getColor(state, world, blockPos);

				if (flags[0])
				{
					flags[0] = false;

					if (FTBChunksClientConfig.waterHeightFactor == 0)
					{
						by = 62;
					}
					else
					{
						by = (by / FTBChunksClientConfig.waterHeightFactor) * FTBChunksClientConfig.waterHeightFactor + FTBChunksClientConfig.waterHeightFactor - 1;
					}

					changed = mapChunk.setHRGB(wx, wz, (by << 24) | color.withTint(Color4I.rgb(BiomeColors.getWaterColor(world, blockPos)).withAlpha(220)).rgb()) | changed;
				}
				else
				{
					changed = mapChunk.setHRGB(wx, wz, (by << 24) | color.rgb()) | changed;
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		if (changed)
		{
			mapChunk.modified = System.currentTimeMillis();
			mapChunk.region.update(true);
		}
	}
}