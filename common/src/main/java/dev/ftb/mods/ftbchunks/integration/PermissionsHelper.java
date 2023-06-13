package dev.ftb.mods.ftbchunks.integration;

import com.google.common.base.Suppliers;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.integration.ftbranks.FTBRanksIntegration;
import dev.ftb.mods.ftbchunks.integration.luckperms.LuckPermsIntegration;

import java.util.function.Supplier;

public class PermissionsHelper {
    public static final String MAX_CLAIMED_PERM = "ftbchunks.max_claimed";
    public static final String MAX_FORCE_LOADED_PERM = "ftbchunks.max_force_loaded";
    public static final String CHUNK_LOAD_OFFLINE_PERM = "ftbchunks.chunk_load_offline";
    public static final String NO_WILDERNESS_PERM = "ftbchunks.no_wilderness";

    private static final Supplier<PermissionsProvider> INSTANCE = Suppliers.memoize(() -> {
                if (Platform.isModLoaded("ftbranks")) {
                    return new FTBRanksIntegration();
                } else if (Platform.isModLoaded("luckperms")) {
                    return new LuckPermsIntegration();
                } else {
                    return new PermissionsProvider() { };
                }
            }
    );

    public static PermissionsProvider getInstance() {
        return INSTANCE.get();
    }
}
