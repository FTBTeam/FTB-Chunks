package com.feed_the_beast.mods.ftbchunks.impl.map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
	public final byte[] height, red, green, blue;
	public final int color;
	public boolean loaded;

	MapChunk(MapRegion r, XZ p)
	{
		region = r;
		pos = p;
		height = new byte[256];
		red = new byte[256];
		green = new byte[256];
		blue = new byte[256];
		color = 0xFF000000;
		loaded = false;
	}

	public int getHeight(int x, int z)
	{
		return height[index(x, z)] & 0xFF;
	}

	public boolean setHeight(int x, int z, int h)
	{
		byte b = (byte) h;
		int i = index(x, z);

		if (height[i] != b)
		{
			height[i] = b;
			region.save = true;
			return true;
		}

		return false;
	}

	public int getRGB(int x, int z)
	{
		int i = index(x, z);
		int r = red[i] & 0xFF;
		int g = green[i] & 0xFF;
		int b = blue[i] & 0xFF;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public boolean setRGB(int x, int z, int rgb)
	{
		int i = index(x, z);
		byte r = (byte) (rgb >> 16);
		byte g = (byte) (rgb >> 8);
		byte b = (byte) rgb;

		if (red[i] != r || green[i] != g || blue[i] != b)
		{
			red[i] = r;
			green[i] = g;
			blue[i] = b;
			region.save = true;
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

		if (b instanceof TallGrassBlock)
		{
			return true;
		}
		else if (b instanceof TorchBlock)
		{
			return !(b instanceof RedstoneTorchBlock);
		}

		return b.isAir(state, world, pos);
	}
}