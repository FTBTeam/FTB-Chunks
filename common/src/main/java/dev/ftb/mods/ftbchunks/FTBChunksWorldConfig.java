package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.api.ProtectionPolicy;
import dev.ftb.mods.ftbchunks.data.AllyMode;
import dev.ftb.mods.ftbchunks.data.ChunkTeamDataImpl;
import dev.ftb.mods.ftbchunks.data.ForceLoadMode;
import dev.ftb.mods.ftbchunks.data.PartyLimitMode;
import dev.ftb.mods.ftbchunks.integration.PermissionsHelper;
import dev.ftb.mods.ftbchunks.integration.stages.StageHelper;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.snbt.config.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;

public interface FTBChunksWorldConfig {
	SNBTConfig CONFIG = SNBTConfig.create(FTBChunks.MOD_ID + "-world");

	EnumValue<ProtectionPolicy> FAKE_PLAYERS = CONFIG.addEnum("fake_players", NameMap.of(ProtectionPolicy.CHECK, ProtectionPolicy.values()).create()).comment("Override to disable/enable fake players like miners and auto-clickers globally.","Default will check this setting for each team");
	IntValue MAX_CLAIMED_CHUNKS = CONFIG.addInt("max_claimed_chunks", 500).comment("Max claimed chunks.", "You can override this with FTB Ranks 'ftbchunks.max_claimed' permission");
	IntValue MAX_FORCE_LOADED_CHUNKS = CONFIG.addInt("max_force_loaded_chunks", 25).comment("Max force loaded chunks.", "You can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission");
	EnumValue<ForceLoadMode> FORCE_LOAD_MODE = CONFIG.addEnum("force_load_mode", ForceLoadMode.NAME_MAP).comment("Control how force-loaded chunks work.","NEVER: only allow chunk force-loading if the owning team has at least one online player.","ALWAYS: always allow force-loading, even if no players are online.","DEFAULT: allow force-loading IF the team has at least one player with the 'ftbchunks.chunk_load_offline' FTB Ranks permission.");
	BooleanValue DISABLE_PROTECTION = CONFIG.addBoolean("disable_protection", false).comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for force-loading");
	EnumValue<AllyMode> ALLY_MODE = CONFIG.addEnum("ally_mode", AllyMode.NAME_MAP).comment("Forced modes won't let players change their ally settings");
	StringListValue CLAIM_DIMENSION_BLACKLIST = CONFIG.addStringList("claim_dimension_blacklist", Collections.emptyList()).comment("Dimension ID's where chunks may not be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End, or \"othermod:*\" to disable chunk claiming in *all* dimensions added by \"othermod\"");
	StringListValue CLAIM_DIMENSION_WHITELIST = CONFIG.addStringList("claim_dimension_whitelist", Collections.emptyList()).comment("Dimension ID's where chunks may be claimed. If non-empty, chunks may be claimed *only* in these dimensions (and the dimension is not in \"claim_dimension_blacklist\"). Same syntax as for \"claim_dimension_blacklist\".");
	BooleanValue NO_WILDERNESS = CONFIG.addBoolean("no_wilderness", false).comment("Requires you to claim chunks in order to edit and interact with blocks");
	BooleanValue FORCE_DISABLE_MINIMAP = CONFIG.addBoolean("force_disable_minimap", false).comment("Minimap for clients connecting to this server will be disabled");
	DoubleValue MAX_IDLE_DAYS_BEFORE_UNCLAIM = CONFIG.addDouble("max_idle_days_before_unclaim", 0D, 0D, 3650D).comment("Maximum time (in real-world days) where if no player in a team logs in, the team automatically loses their claims.", "Prevents chunks being claimed indefinitely by teams who no longer play.","Default of 0 means no automatic loss of claims.");
	DoubleValue MAX_IDLE_DAYS_BEFORE_UNFORCE = CONFIG.addDouble("max_idle_days_before_unforce", 0D, 0D, 3650D).comment("Maximum time (in real-world days) where if no player in a team logs in, any forceloaded chunks owned by the team are no longer forceloaded.", "Prevents chunks being forceloaded indefinitely by teams who no longer play.","Default of 0 means no automatic loss of forceloading.");
	IntValue LONG_RANGE_TRACKER_INTERVAL = CONFIG.addInt("long_range_tracker_interval", 20, 0, Integer.MAX_VALUE).comment("Interval in ticks to send updates to clients with long-range player tracking data.","Lower values mean more frequent updates but more server load and network traffic; be careful with this, especially on busy servers.","Setting this to 0 disables long-range tracking.");
	BooleanValue PROTECT_UNKNOWN_EXPLOSIONS = CONFIG.addBoolean("protect_unknown_explosions", true).comment("When true, standard FTB Chunk explosion protection is applied in protected chunks when the source of the explosion cannot be determined","(Ghast fireballs are a common case - vanilla supplies a null entity source)");
	IntValue HARD_TEAM_CLAIM_LIMIT = CONFIG.addInt("hard_team_claim_limit", 0, 0, Integer.MAX_VALUE).comment("Hard limit for the number of chunks a team can claim, regardless of how many members. Default of 0 means no hard limit.");
	IntValue HARD_TEAM_FORCE_LIMIT = CONFIG.addInt("hard_team_force_limit", 0, 0, Integer.MAX_VALUE).comment("Hard limit for the number of chunks a team can force-load, regardless of how many members. Default of 0 means no hard limit.");
	EnumValue<PartyLimitMode> PARTY_LIMIT_MODE = CONFIG.addEnum("party_limit_mode", PartyLimitMode.NAME_MAP).comment("Method by which party claim & force-load limits are calculated.","LARGEST: use the limits of the member with the largest limits","SUM: add up all the members' limits","OWNER: use the party owner's limits only","AVERAGE: use the average of all members' limits.");
	BooleanValue REQUIRE_GAME_STAGE = CONFIG.addBoolean("require_game_stage", false).comment("If true, the player must have the 'ftbchunks_mapping' Game stage to be able to use the map and minimap.\nRequires KubeJS and/or Gamestages to be installed.");
	BooleanValue LOCATION_MODE_OVERRIDE = CONFIG.addBoolean("location_mode_override", false).comment("If true, \"Location Visibility\" team settings are ignored, and all players can see each other anywhere on the map.");

	static int getMaxClaimedChunks(ChunkTeamDataImpl playerData, ServerPlayer player) {
		if (player != null) {
			return PermissionsHelper.getInstance().getMaxClaimedChunks(player, MAX_CLAIMED_CHUNKS.get()) + playerData.getExtraClaimChunks();
		}

		return MAX_CLAIMED_CHUNKS.get() + playerData.getExtraClaimChunks();
	}

	static int getMaxForceLoadedChunks(ChunkTeamDataImpl playerData, ServerPlayer player) {
		if (player != null) {
			return PermissionsHelper.getInstance().getMaxForceLoadedChunks(player, MAX_FORCE_LOADED_CHUNKS.get()) + playerData.getExtraForceLoadChunks();
		}

		return MAX_FORCE_LOADED_CHUNKS.get() + playerData.getExtraForceLoadChunks();
	}

	static boolean canPlayerOfflineForceload(ServerPlayer player) {
		// note: purely checking the player's own permission here; not interested in server defaults or party data
		return player != null && PermissionsHelper.getInstance().getChunkLoadOffline(player, false);
	}

	static boolean noWilderness(ServerPlayer player) {
		if (player != null) {
			return PermissionsHelper.getInstance().getNoWilderness(player, NO_WILDERNESS.get());
		}

		return NO_WILDERNESS.get();
	}

	static boolean playerHasMapStage(Player player) {
		return !REQUIRE_GAME_STAGE.get() || StageHelper.INSTANCE.get().has(player, "ftbchunks_mapping");
	}

	static boolean shouldShowMinimap(Player player) {
		return !FORCE_DISABLE_MINIMAP.get() && playerHasMapStage(player);
	}

}