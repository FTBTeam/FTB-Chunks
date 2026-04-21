package dev.ftb.mods.ftbchunks.util.platform;

import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

@FunctionalInterface
public interface ForceLoadHandler {
    void updateForceLoadingForChunk(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add);
}
