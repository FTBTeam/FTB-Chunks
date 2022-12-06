package dev.ftb.mods.ftbchunks;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import dev.ftb.mods.ftbranks.api.RankManager;
import dev.ftb.mods.ftbranks.api.event.*;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class FTBRanksIntegration {
	private static final String MAX_CLAIMED_PERM = "ftbchunks.max_claimed";
	private static final String MAX_FORCE_LOADED_PERM = "ftbchunks.max_force_loaded";
	private static final String CHUNK_LOAD_OFFLINE_PERM = "ftbchunks.chunk_load_offline";

	public static int getMaxClaimedChunks(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, MAX_CLAIMED_PERM).asInteger().orElse(def), 0);
	}

	public static int getMaxForceLoadedChunks(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, MAX_FORCE_LOADED_PERM).asInteger().orElse(def), 0);
	}

	public static boolean getChunkLoadOffline(ServerPlayer player, boolean def) {
		return FTBRanksAPI.getPermissionValue(player, CHUNK_LOAD_OFFLINE_PERM).asBoolean().orElse(def);
	}

	public static boolean getNoWilderness(ServerPlayer player, boolean def) {
		return FTBRanksAPI.getPermissionValue(player, "ftbchunks.no_wilderness").asBoolean().orElse(def);
	}

	public static void registerEvents() {
		RankEvent.ADD_PLAYER.register(FTBRanksIntegration::playerAdded);
		RankEvent.REMOVE_PLAYER.register(FTBRanksIntegration::playerRemoved);
		RankEvent.PERMISSION_CHANGED.register(FTBRanksIntegration::permissionSet);
		RankEvent.RELOADED.register(FTBRanksIntegration::ranksReloaded);
		RankEvent.CONDITION_CHANGED.register(FTBRanksIntegration::conditionChanged);
	}


	// ---------------------- event listeners below here ------------------
	private static void playerAdded(PlayerAddedToRankEvent event) {
		updateForPlayer(event.getManager(), event.getPlayer());
	}

	private static void playerRemoved(PlayerRemovedFromRankEvent event) {
		updateForPlayer(event.getManager(), event.getPlayer());
	}

	private static void permissionSet(PermissionNodeChangedEvent event) {
		String node = event.getNode();
		if (node.equals(MAX_CLAIMED_PERM) || node.equals(MAX_FORCE_LOADED_PERM) || node.equals(CHUNK_LOAD_OFFLINE_PERM)) {
			updateAll(event.getManager());
		}
	}

	private static void ranksReloaded(RanksReloadedEvent event) {
		updateAll(event.getManager());
	}

	private static void conditionChanged(ConditionChangedEvent event) {
		updateAll(event.getManager());
	}

	private static void updateAll(RankManager manager) {
		if (FTBChunksAPI.isManagerLoaded()) {
			manager.getServer().getPlayerList().getPlayers().forEach(player -> {
				FTBChunksTeamData data = FTBChunksAPI.getManager().getData(player);
				data.setForceLoadMember(player.getUUID(), getChunkLoadOffline(player, false));
			});
			FTBTeamsAPI.getManager().getTeams().forEach(team -> FTBChunksAPI.getManager().getData(team).updateLimits());
		}
	}

	private static void updateForPlayer(RankManager manager, GameProfile profile) {
		Team team = FTBTeamsAPI.getPlayerTeam(profile.getId());
		if (team != null) {
			FTBChunksTeamData teamData = FTBChunksAPI.getManager().getData(team);
			ServerPlayer player = manager.getServer().getPlayerList().getPlayer(profile.getId());
			if (player != null) {
				teamData.setForceLoadMember(player.getUUID(), getChunkLoadOffline(player, false));
			}
			teamData.updateLimits();
		}
	}
}