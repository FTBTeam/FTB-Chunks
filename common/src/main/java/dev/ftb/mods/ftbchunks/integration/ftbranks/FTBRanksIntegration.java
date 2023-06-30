package dev.ftb.mods.ftbchunks.integration.ftbranks;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.integration.PermissionsHelper;
import dev.ftb.mods.ftbchunks.integration.PermissionsProvider;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import dev.ftb.mods.ftbranks.api.RankManager;
import dev.ftb.mods.ftbranks.api.event.*;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;

import static dev.ftb.mods.ftbchunks.integration.PermissionsHelper.*;

public class FTBRanksIntegration implements PermissionsProvider {
	@Override
	public int getMaxClaimedChunks(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, MAX_CLAIMED_PERM).asInteger().orElse(def), 0);
	}

	@Override
	public int getMaxForceLoadedChunks(ServerPlayer player, int def) {
		return Math.max(FTBRanksAPI.getPermissionValue(player, MAX_FORCE_LOADED_PERM).asInteger().orElse(def), 0);
	}

	@Override
	public boolean getChunkLoadOffline(ServerPlayer player, boolean def) {
		return FTBRanksAPI.getPermissionValue(player, CHUNK_LOAD_OFFLINE_PERM).asBoolean().orElse(def);
	}

	@Override
	public boolean getNoWilderness(ServerPlayer player, boolean def) {
		return FTBRanksAPI.getPermissionValue(player, NO_WILDERNESS_PERM).asBoolean().orElse(def);
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
				FTBChunksTeamData data = FTBChunksAPI.getManager().getOrCreateData(player);
				data.setForceLoadMember(player.getUUID(), PermissionsHelper.getInstance().getChunkLoadOffline(player, false));
			});
			FTBTeamsAPI.api().getManager().getTeams().forEach(team -> FTBChunksAPI.getManager().getOrCreateData(team).updateLimits());
		}
	}

	private static void updateForPlayer(RankManager manager, GameProfile profile) {
		FTBTeamsAPI.api().getManager().getTeamForPlayerID(profile.getId()).ifPresent(team -> {
			FTBChunksTeamData teamData = FTBChunksAPI.getManager().getOrCreateData(team);
			ServerPlayer player = manager.getServer().getPlayerList().getPlayer(profile.getId());
			if (player != null) {
				teamData.setForceLoadMember(player.getUUID(), PermissionsHelper.getInstance().getChunkLoadOffline(player, false));
			}
			teamData.updateLimits();
		});
	}
}