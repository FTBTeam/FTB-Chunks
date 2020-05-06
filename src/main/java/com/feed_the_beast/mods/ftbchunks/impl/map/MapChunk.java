package com.feed_the_beast.mods.ftbchunks.impl.map;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.TallGrassBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.IChunk;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class MapChunk
{
	public static int index(int x, int z)
	{
		return (x & 0xF) + (z & 0xF) * 16;
	}

	public final MapRegion region;
	public final XZ pos;
	public boolean loaded;
	public boolean weakUpdate;

	MapChunk(MapRegion r, XZ p)
	{
		region = r;
		pos = p;
		loaded = false;
		weakUpdate = false;
	}

	public int getHeight(int x, int z)
	{
		return (region.getImage().getRGB(pos.x * 16 + x, pos.z * 16 + z) >> 24) & 0xFF;
	}

	public int getRGB(int x, int z)
	{
		return 0xFF000000 | region.getImage().getRGB(pos.x * 16 + x, pos.z * 16 + z);
	}

	public boolean setHRGB(int x, int z, int hrgb)
	{
		int c = region.getImage().getRGB(pos.x * 16 + x, pos.z * 16 + z);

		if (c != hrgb)
		{
			region.getImage().setRGB(pos.x * 16 + x, pos.z * 16 + z, hrgb);
			region.save();
			return true;
		}

		return false;
	}

	public static int getHeight(@Nullable IChunk chunk, BlockPos.Mutable currentBlockPos, int blockX, int blockZ, int topY)
	{
		if (topY == -1 || chunk == null || chunk.getWorldForge() == null)
		{
			return -1;
		}

		for (int by = topY; by > 0; by--)
		{
			currentBlockPos.setPos(blockX, by, blockZ);
			BlockState state = chunk.getBlockState(currentBlockPos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK)
			{
				for (; by > 0; by--)
				{
					currentBlockPos.setPos(blockX, by, blockZ);
					state = chunk.getBlockState(currentBlockPos);

					if (state.getBlock().isAir(state, chunk.getWorldForge(), currentBlockPos))
					{
						break;
					}
				}
			}

			if (!skipBlock(state, chunk.getWorldForge(), currentBlockPos))
			{
				return by;
			}
		}

		return -1;
	}

	public static boolean skipBlock(BlockState state, IWorld world, BlockPos pos)
	{
		Block b = state.getBlock();

		if (b == Blocks.TALL_GRASS || b == Blocks.LARGE_FERN || b instanceof TallGrassBlock)
		{
			return true;
		}
		else if (b instanceof FireBlock)
		{
			return true;
		}
		else if (b instanceof AbstractButtonBlock)
		{
			return true;
		}
		else if (b instanceof TorchBlock)
		{
			return !(b instanceof RedstoneTorchBlock);
		}

		return b.isAir(state, world, pos);
	}

	public XZ getActualPos()
	{
		return XZ.of((region.pos.x << 5) + pos.x, (region.pos.z << 5) + pos.z);
	}
}