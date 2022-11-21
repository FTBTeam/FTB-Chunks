package dev.ftb.mods.ftbchunks.fabric;

import dev.ftb.mods.ftbchunks.FTBChunks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class FTBChunksExpectedImpl {
    public static void addChunkToForceLoaded(ServerLevel level, String modId, UUID owner, int chunkX, int chunkY, boolean add) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkY);
        level.setChunkForced(chunkX, chunkY, add);
        FTBChunks.LOGGER.debug("set force-loading for chunk ({},{}) = {}", chunkX, chunkY, add);
        if (add) {
            level.getChunkSource().addRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
        } else {
            level.getChunkSource().removeRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
        }
    }
}
