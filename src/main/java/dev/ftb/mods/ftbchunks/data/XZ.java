package dev.ftb.mods.ftbchunks.data;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * @author LatvianModder
 */
public class XZ {
	public static XZ of(int x, int z) {
		return new XZ(x, z);
	}

	public static XZ of(ChunkPos pos) {
		return of(pos.x, pos.z);
	}

	public static XZ chunkFromBlock(int x, int z) {
		return of(x >> 4, z >> 4);
	}

	public static XZ chunkFromBlock(Vec3i pos) {
		return chunkFromBlock(pos.getX(), pos.getZ());
	}

	public static XZ regionFromChunk(int x, int z) {
		return of(x >> 5, z >> 5);
	}

	public static XZ regionFromChunk(ChunkPos p) {
		return of(p.x >> 5, p.z >> 5);
	}

	public static XZ regionFromBlock(int x, int z) {
		return of(x >> 9, z >> 9);
	}

	public final int x;
	public final int z;

	private XZ(int _x, int _z) {
		x = _x;
		z = _z;
	}

	public int hashCode() {
		int x1 = 1664525 * x + 1013904223;
		int z1 = 1664525 * (z ^ -559038737) + 1013904223;
		return x1 ^ z1;
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (!(o instanceof XZ)) {
			return false;
		} else {
			XZ p = (XZ) o;
			return x == p.x && z == p.z;
		}
	}

	public String toString() {
		return "[" + x + ", " + z + "]";
	}

	public ChunkDimPos dim(ResourceKey<Level> type) {
		return new ChunkDimPos(type, x, z);
	}

	public ChunkDimPos dim(Level world) {
		return dim(world.dimension());
	}

	public XZ offset(int ox, int oz) {
		return of(x + ox, z + oz);
	}

	public long asLong() {
		return (long) x & 4294967295L | ((long) z & 4294967295L) << 32L;
	}

	public String toRegionString() {
		return String.format("%05X-%05X", x + 60000, z + 60000);
	}
}