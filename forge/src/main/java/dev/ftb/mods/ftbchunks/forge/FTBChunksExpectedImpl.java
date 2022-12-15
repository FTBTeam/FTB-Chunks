package dev.ftb.mods.ftbchunks.forge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.UUID;

public class FTBChunksExpectedImpl {
    public static void addChunkToForceLoaded(ServerLevel level, String modId, UUID owner, int chunkX, int chunkY, boolean add) {
        ForgeChunkManager.forceChunk(level, modId, owner, chunkX, chunkY, add, true);
        FTBChunks.LOGGER.debug("force chunk (forge): mod={} owner={}, c=({},{}), load={}", modId, owner, chunkX, chunkY, add);
    }
}
