package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class ChunkDimPos
{
	public final DimensionType dimension;
	public final int x;
	public final int z;
	private ChunkPos chunkPos;
	private int hash;

	public ChunkDimPos(DimensionType dim, int _x, int _z)
	{
		dimension = dim;
		x = _x;
		z = _z;
	}

	public ChunkDimPos(DimensionType dim, ChunkPos pos)
	{
		this(dim, pos.x, pos.z);
	}

	public ChunkDimPos(IWorld world, BlockPos pos)
	{
		this(world.getDimension().getType(), pos.getX() >> 4, pos.getZ() >> 4);
	}

	public ChunkDimPos(Entity entity)
	{
		this(entity.dimension, MathHelper.floor(entity.getPosX()) >> 4, MathHelper.floor(entity.getPosZ()) >> 4);
	}

	public ChunkPos getChunkPos()
	{
		if (chunkPos == null)
		{
			chunkPos = new ChunkPos(x, z);
		}

		return chunkPos;
	}

	@Override
	public String toString()
	{
		return "[" + DimensionType.getKey(dimension) + ":" + x + ":" + z + "]";
	}

	@Override
	public int hashCode()
	{
		if (hash == 0)
		{
			hash = Objects.hash(dimension, x, z);

			if (hash == 0)
			{
				hash = 1;
			}
		}

		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		else if (obj instanceof ChunkDimPos)
		{
			ChunkDimPos p = (ChunkDimPos) obj;
			return dimension == p.dimension && x == p.x && z == p.z;
		}

		return false;
	}
}