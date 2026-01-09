package dev.ftb.mods.ftbchunks.util;

import net.minecraft.world.level.ChunkPos;

/**
 * Helper record to store chunk position along with a minimum Y value.
 */
public record ChunkPosWithMinY(int chunkX, int chunkZ, int minY) {
    public ChunkPos asChunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }

    public long chunkPosAsLong() {
        return asChunkPos().toLong();
    }
}
