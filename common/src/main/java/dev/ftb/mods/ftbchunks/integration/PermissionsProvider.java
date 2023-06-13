package dev.ftb.mods.ftbchunks.integration;

import net.minecraft.server.level.ServerPlayer;

public interface PermissionsProvider {
    default int getMaxClaimedChunks(ServerPlayer player, int def) {
        return def;
    }

    default int getMaxForceLoadedChunks(ServerPlayer player, int def) {
        return def;
    }

    default boolean getChunkLoadOffline(ServerPlayer player, boolean def) {
        return def;
    }

    default boolean getNoWilderness(ServerPlayer player, boolean def) {
        return def;
    }
}
