package dev.ftb.mods.ftbchunks;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbteams.api.event.TeamCollectPropertiesEvent;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public class FTBChunksExpected {
    @ExpectPlatform
    public static void addChunkToForceLoaded(ServerLevel level, String modId, UUID owner, int chunkX, int chunkY, boolean add) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void getPlatformSpecificProperties(TeamCollectPropertiesEvent event) {
        // required since Forge & Fabric have different requirements:
        // https://github.com/FTBTeam/FTB-Chunks/pull/213
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Protection getBlockPlaceProtection() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Protection getBlockInteractProtection() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Protection getBlockBreakProtection() {
        throw new AssertionError();
    }
}
