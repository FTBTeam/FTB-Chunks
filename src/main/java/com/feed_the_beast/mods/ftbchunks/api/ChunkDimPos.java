package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class ChunkDimPos implements Comparable<ChunkDimPos> {
	public final ResourceKey<Level> dimension;
	public final int x;
	public final int z;
	private ChunkPos chunkPos;
	private int hash;

	public ChunkDimPos(ResourceKey<Level> dim, int _x, int _z) {
		dimension = dim;
		x = _x;
		z = _z;
	}

	public ChunkDimPos(ResourceKey<Level> dim, ChunkPos pos) {
		this(dim, pos.x, pos.z);
	}

	public ChunkDimPos(Level world, BlockPos pos) {
		this(world.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
	}

	public ChunkDimPos(Entity entity) {
		this(entity.level, entity.blockPosition());
	}

	public ChunkPos getChunkPos() {
		if (chunkPos == null) {
			chunkPos = new ChunkPos(x, z);
		}

		return chunkPos;
	}

	@Override
	public String toString() {
		return "[" + dimension.location() + ":" + x + ":" + z + "]";
	}

	@Override
	public int hashCode() {
		if (hash == 0) {
			hash = Objects.hash(dimension.location(), x, z);

			if (hash == 0) {
				hash = 1;
			}
		}

		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof ChunkDimPos) {
			ChunkDimPos p = (ChunkDimPos) obj;
			return dimension == p.dimension && x == p.x && z == p.z;
		}

		return false;
	}

	@Override
	public int compareTo(ChunkDimPos o) {
		int i = dimension.location().compareTo(o.dimension.location());
		return i == 0 ? Long.compare(getChunkPos().toLong(), o.getChunkPos().toLong()) : i;
	}

	public ChunkDimPos offset(int ox, int oz) {
		return new ChunkDimPos(dimension, x + ox, z + oz);
	}
}