package dev.ftb.mods.ftbchunks.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

public class FTBChunksExpectedImpl {
    public static void addChunkToForceLoaded(ServerLevel level, String modId, BlockPos owner, int chunkX, int chunkY, boolean add) {
        ForgeChunkManager.forceChunk(level, modId, owner, chunkX, chunkY, add, true);
    }
}
