package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.entity.player.ServerPlayerEntity;

/**
 * @author LatvianModder
 */
public class FTBRanksIntegration
{
	public static int getMaxClaimedChunks(ServerPlayerEntity player, int def)
	{
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbchunks.max_claimed").asNumber().orElse(def).intValue(), 0);
	}

	public static int getMaxForceLoadedChunks(ServerPlayerEntity player, int def)
	{
		return Math.max(FTBRanksAPI.getPermissionValue(player, "ftbchunks.max_force_loaded").asNumber().orElse(def).intValue(), 0);
	}

	public static boolean canSeeMap(ServerPlayerEntity player)
	{
		return FTBRanksAPI.getPermissionValue(player, "ftbchunks.see_all_map").asBoolean().orElseGet(() -> player.hasPermissionLevel(2));
	}
}