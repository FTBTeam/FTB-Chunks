package dev.ftb.mods.ftbchunks.integration.luckperms;

import dev.ftb.mods.ftbchunks.integration.PermissionsProvider;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

import static dev.ftb.mods.ftbchunks.integration.PermissionsHelper.*;

public class LuckPermsIntegration implements PermissionsProvider {
    @Override
    public int getMaxClaimedChunks(ServerPlayer player, int def) {
        return Math.max(getMetaData(player.getUUID(), MAX_CLAIMED_PERM).map(Integer::parseInt).orElse(def), 0);
    }

    @Override
    public int getMaxForceLoadedChunks(ServerPlayer player, int def) {
        return Math.max(getMetaData(player.getUUID(), MAX_FORCE_LOADED_PERM).map(Integer::parseInt).orElse(def), 0);
    }

    @Override
    public boolean getChunkLoadOffline(ServerPlayer player, boolean def) {
        return getMetaData(player.getUUID(), CHUNK_LOAD_OFFLINE_PERM).map(Boolean::parseBoolean).orElse(def);
    }

    @Override
    public boolean getNoWilderness(ServerPlayer player, boolean def) {
        return getMetaData(player.getUUID(), NO_WILDERNESS_PERM).map(Boolean::parseBoolean).orElse(def);
    }

    private static Optional<String> getMetaData(UUID uuid, String meta) {
        LuckPerms luckperms = LuckPermsProvider.get();
        Optional<String> metaValue = Optional.empty();
        try {
            User user = luckperms.getUserManager().getUser(uuid);
            if (user != null) {
                Optional<QueryOptions> context = luckperms.getContextManager().getQueryOptions(user);
                if (context.isPresent()) {
                    metaValue = Optional.ofNullable(user.getCachedData().getMetaData(context.get()).getMetaValue(meta));
                }
            }
        } catch (IllegalStateException e) {
            System.err.println("Error on fetching user with luckperms");
            System.err.println(e.getMessage());
        }
        return metaValue;
    }
}
