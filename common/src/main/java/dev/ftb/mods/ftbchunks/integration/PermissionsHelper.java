package dev.ftb.mods.ftbchunks.integration;

import dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper;
import net.minecraft.server.level.ServerPlayer;

public class PermissionsHelper {
    public static final String MAX_CLAIMED_PERM = "ftbchunks.max_claimed";
    public static final String MAX_FORCE_LOADED_PERM = "ftbchunks.max_force_loaded";
    public static final String CHUNK_LOAD_OFFLINE_PERM = "ftbchunks.chunk_load_offline";
    public static final String NO_WILDERNESS_PERM = "ftbchunks.no_wilderness";

    public static int getMaxClaimedChunks(ServerPlayer player, int def) {
        return PermissionHelper.getInstance().getProvider().getIntegerPermission(player, MAX_CLAIMED_PERM, def);
    }

    public static int getMaxForceLoadedChunks(ServerPlayer player, int def) {
        return PermissionHelper.getInstance().getProvider().getIntegerPermission(player, MAX_FORCE_LOADED_PERM, def);
    }

    public static boolean getChunkLoadOffline(ServerPlayer player, boolean def) {
        return PermissionHelper.getInstance().getProvider().getBooleanPermission(player, CHUNK_LOAD_OFFLINE_PERM, def);
    }

    public static boolean getNoWilderness(ServerPlayer player, boolean def) {
        return PermissionHelper.getInstance().getProvider().getBooleanPermission(player, NO_WILDERNESS_PERM, def);
    }
}
