package dev.ftb.mods.ftbchunks;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.ftb.mods.ftbchunks.data.Protection;
import dev.ftb.mods.ftbteams.event.TeamCollectPropertiesEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class FTBChunksExpected {
    @ExpectPlatform
    public static void addChunkToForceLoaded(ServerLevel level, String modId, UUID owner, int chunkX, int chunkY, boolean add) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void getTeamConfigsForPlatform(TeamCollectPropertiesEvent event) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Protection getBlockPlaceProtectionForPlatform() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Protection getBlockInteractProtectionForPlatform() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Protection getBlockBreakProtectionForPlatform() {
        throw new AssertionError();
    }
}
