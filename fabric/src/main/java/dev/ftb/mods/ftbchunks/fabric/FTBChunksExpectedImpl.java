package dev.ftb.mods.ftbchunks.fabric;

import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class FTBChunksExpectedImpl {
    public static void addChunkToForceLoaded(ServerLevel level, String modId, UUID owner, int chunkX, int chunkY, boolean add) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkY);
        if (add) {
            level.getChunkSource().addRegionTicket(FTBChunksAPI.FORCE_LOADED_TICKET, chunkPos, 2, chunkPos);
        } else {
            level.getChunkSource().removeRegionTicket(FTBChunksAPI.FORCE_LOADED_TICKET, chunkPos, 2, chunkPos);
        }
    }
}
