package dev.ftb.mods.ftbchunks.forge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.data.Protection;
import dev.ftb.mods.ftbteams.event.TeamCollectPropertiesEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.UUID;

public class FTBChunksExpectedImpl {
    public static void addChunkToForceLoaded(ServerLevel level, String modId, UUID owner, int chunkX, int chunkY, boolean add) {
        ForgeChunkManager.forceChunk(level, modId, owner, chunkX, chunkY, add, true);
        FTBChunks.LOGGER.debug("force chunk (forge): mod={} owner={}, c=({},{}), load={}", modId, owner, chunkX, chunkY, add);
    }

    public static void getPlatformSpecificProperties(TeamCollectPropertiesEvent event) {
        event.add(FTBChunksTeamData.BLOCK_EDIT_MODE);
        event.add(FTBChunksTeamData.BLOCK_INTERACT_MODE);
    }

    public static Protection getBlockPlaceProtection() {
        return Protection.EDIT_BLOCK;
    }

    public static Protection getBlockInteractProtection() {
        return Protection.INTERACT_BLOCK;
    }

    public static Protection getBlockBreakProtection() {
        return Protection.EDIT_BLOCK;
    }
}
