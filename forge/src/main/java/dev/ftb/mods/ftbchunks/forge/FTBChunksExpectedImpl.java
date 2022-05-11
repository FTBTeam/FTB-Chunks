package dev.ftb.mods.ftbchunks.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.UUID;

public class FTBChunksExpectedImpl {
    public static void addChunkToForceLoaded(ServerLevel level, String modId, BlockPos owner, int chunkX, int chunkY, boolean add) {
        ForgeChunkManager.forceChunk(level, modId, UUID.randomUUID(), chunkX, chunkY, add, false);
    }
}
