package dev.ftb.mods.ftbchunks.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;

/**
 * Helper record to store chunk position along with a minimum Y value.
 */
public record ChunkPosWithMinY(int chunkX, int chunkZ, int minY) {
    public static final Codec<ChunkPosWithMinY> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(ChunkPosWithMinY::chunkX),
            Codec.INT.fieldOf("z").forGetter(ChunkPosWithMinY::chunkZ),
            Codec.INT.fieldOf("min_y").forGetter(ChunkPosWithMinY::minY)
    ).apply(instance, ChunkPosWithMinY::new));

    public ChunkPos asChunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }

    public long chunkPosAsLong() {
        return asChunkPos().toLong();
    }
}
