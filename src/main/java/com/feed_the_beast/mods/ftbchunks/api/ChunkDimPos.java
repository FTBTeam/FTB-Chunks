package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author LatvianModder
 */
public class ChunkDimPos implements Comparable<ChunkDimPos>
{
	public static String getID(@Nullable DimensionType type)
	{
		if (type == null)
		{
			return "";
		}

		ResourceLocation id = type.getRegistryName();

		if (id == null)
		{
			id = DimensionType.getKey(type);
		}

		return id == null ? "" : id.toString();
	}

	public static String getID(@Nullable IWorld world)
	{
		return world == null ? "" : getID(world.getDimension().getType());
	}

	@Nullable
	public static ServerWorld getWorld(MinecraftServer server, String id)
	{
		DimensionType type = id.isEmpty() ? null : DimensionType.byName(new ResourceLocation(id));
		return type == null ? null : server.getWorld(type);
	}

	public final String dimension;
	public final int x;
	public final int z;
	private ChunkPos chunkPos;
	private int hash;

	public ChunkDimPos(String dim, int _x, int _z)
	{
		dimension = dim;
		x = _x;
		z = _z;
	}

	public ChunkDimPos(String dim, ChunkPos pos)
	{
		this(dim, pos.x, pos.z);
	}

	public ChunkDimPos(IWorld world, BlockPos pos)
	{
		this(Objects.requireNonNull(getID(world)), pos.getX() >> 4, pos.getZ() >> 4);
	}

	public ChunkDimPos(Entity entity)
	{
		this(entity.world, entity.getPosition());
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
		return "[" + dimension + ":" + x + ":" + z + "]";
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
			return dimension.equals(p.dimension) && x == p.x && z == p.z;
		}

		return false;
	}

	@Override
	public int compareTo(ChunkDimPos o)
	{
		int i = dimension.compareTo(o.dimension);
		return i == 0 ? Long.compare(getChunkPos().asLong(), o.getChunkPos().asLong()) : i;
	}

	public ChunkDimPos offset(int ox, int oz)
	{
		return new ChunkDimPos(dimension, x + ox, z + oz);
	}
}