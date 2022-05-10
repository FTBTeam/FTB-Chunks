package dev.ftb.mods.ftbchunks;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class FTBChunksExpected {
    @ExpectPlatform
    public static void addChunkToForceLoaded(ServerLevel level, String modId, BlockPos owner, int chunkX, int chunkY, boolean add) {
        throw new AssertionError();
    }
}
