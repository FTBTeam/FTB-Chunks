package dev.ftb.mods.ftbchunks;

import com.feed_the_beast.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class FTBRanksIntegration {
	public static int getMaxClaimedChunks(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbchunks.max_claimed").asInteger().orElse(def), 0);
	}

	public static int getMaxForceLoadedChunks(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbchunks.max_force_loaded").asInteger().orElse(def), 0);
	}

	public static boolean getChunkLoadOffline(ServerPlayer player, boolean def) {
		return FTBRanksAPI.getPermissionValue(player, "ftbchunks.chunk_load_offline").asBoolean().orElse(def);
	}
}