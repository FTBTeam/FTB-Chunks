package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.data.*;
import dev.ftb.mods.ftbchunks.integration.PermissionsHelper;
import dev.ftb.mods.ftbchunks.util.DimensionFilter;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;
import dev.ftb.mods.ftblibrary.snbt.config.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;

/**
 * @author LatvianModder
 */
public interface FTBChunksWorldConfig {
	SNBTConfig CONFIG = SNBTConfig.create(FTBChunks.MOD_ID + "-world");

	EnumValue<ProtectionOverride> FAKE_PLAYERS = CONFIG.getEnum("fake_players", NameMap.of(ProtectionOverride.CHECK, ProtectionOverride.values()).create()).comment("Override to disable/enable fake players like miners and auto-clickers globally.","Default will check this setting for each team");
	IntValue MAX_CLAIMED_CHUNKS = CONFIG.getInt("max_claimed_chunks", 500).comment("Max claimed chunks.", "You can override this with FTB Ranks 'ftbchunks.max_claimed' permission");
	IntValue MAX_FORCE_LOADED_CHUNKS = CONFIG.getInt("max_force_loaded_chunks", 25).comment("Max force loaded chunks.", "You can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission");
	BooleanValue CHUNK_LOAD_OFFLINE = CONFIG.getBoolean("chunk_load_offline", true).comment("Fallback offline chunk loading behaviour for when 'force_load_mode' is set to 'default'");
	EnumValue<ForceLoadMode> FORCE_LOAD_MODE = CONFIG.getEnum("force_load_mode", ForceLoadMode.NAME_MAP).comment("Control how force-loaded chunks work.","NEVER: only allow chunk force-loading if the owning team has at least one online player.","ALWAYS: always allow force-loading, even if no players are online.","DEFAULT: allow force-loading IF the team has at least one player with the 'ftbchunks.chunk_load_offline' FTB Ranks permission OR 'chunk_load_offline' is true.");
	BooleanValue DISABLE_PROTECTION = CONFIG.getBoolean("disable_protection", false).comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for force-loading");
	EnumValue<AllyMode> ALLY_MODE = CONFIG.getEnum("ally_mode", AllyMode.NAME_MAP).comment("Forced modes won't let players change their ally settings");
	StringListValue CLAIM_DIMENSION_BLACKLIST = CONFIG.getStringList("claim_dimension_blacklist", Collections.emptyList()).comment("Dimension ID's where chunks may not be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End, or \"othermod:*\" to disable chunk claiming in *all* dimensions added by \"othermod\"");
	StringListValue CLAIM_DIMENSION_WHITELIST = CONFIG.getStringList("claim_dimension_whitelist", Collections.emptyList()).comment("Dimension ID's where chunks may be claimed. If non-empty, chunks may be claimed *only* in these dimensions (and the dimension is not in \"claim_dimension_blacklist\"). Same syntax as for \"claim_dimension_blacklist\".");
	BooleanValue NO_WILDERNESS = CONFIG.getBoolean("no_wilderness", false).comment("Requires you to claim chunks in order to edit and interact with blocks");
	StringListValue NO_WILDERNESS_DIMENSIONS = CONFIG.getStringList("no_wilderness_dimensions", Collections.emptyList()).comment("Dimension ID's where the no_wilderness rule is enforced - building is only allowed in claimed chunks. If this is non-empty, it overrides the 'no_wilderness' setting.");
	BooleanValue FORCE_DISABLE_MINIMAP = CONFIG.getBoolean("force_disable_minimap", false).comment("Minimap for clients connecting to this server will be disabled");
	DoubleValue MAX_IDLE_DAYS_BEFORE_UNCLAIM = CONFIG.getDouble("max_idle_days_before_unclaim", 0D, 0D, 3650D).comment("Maximum time (in real-world days) where if no player in a team logs in, the team automatically loses their claims.", "Prevents chunks being claimed indefinitely by teams who no longer play.","Default of 0 means no automatic loss of claims.");
	DoubleValue MAX_IDLE_DAYS_BEFORE_UNFORCE = CONFIG.getDouble("max_idle_days_before_unforce", 0D, 0D, 3650D).comment("Maximum time (in real-world days) where if no player in a team logs in, any forceloaded chunks owned by the team are no longer forceloaded.", "Prevents chunks being forceloaded indefinitely by teams who no longer play.","Default of 0 means no automatic loss of forceloading.");
	IntValue LONG_RANGE_TRACKER_INTERVAL = CONFIG.getInt("long_range_tracker_interval", 20, 0, Integer.MAX_VALUE).comment("Interval in ticks to send updates to clients with long-range player tracking data.","Lower values mean more frequent updates but more server load and network traffic; be careful with this, especially on busy servers.","Setting this to 0 disables long-range tracking.");
	BooleanValue PROTECT_UNKNOWN_EXPLOSIONS = CONFIG.getBoolean("protect_unknown_explosions", true).comment("When true, standard FTB Chunk explosion protection is applied in protected chunks when the source of the explosion cannot be determined","(Ghast fireballs are a common case - vanilla supplies a null entity source)");
	IntValue HARD_TEAM_CLAIM_LIMIT = CONFIG.getInt("hard_team_claim_limit", 0, 0, Integer.MAX_VALUE).comment("Hard limit for the number of chunks a team can claim, regardless of how many members. Default of 0 means no hard limit.");
	IntValue HARD_TEAM_FORCE_LIMIT = CONFIG.getInt("hard_team_force_limit", 0, 0, Integer.MAX_VALUE).comment("Hard limit for the number of chunks a team can force-load, regardless of how many members. Default of 0 means no hard limit.");
	EnumValue<PartyLimitMode> PARTY_LIMIT_MODE = CONFIG.getEnum("party_limit_mode", PartyLimitMode.NAME_MAP).comment("Method by which party claim & force-load limits are calculated.","LARGEST: use the limits of the member with the largest limits","SUM: add up all the members' limits","OWNER: use the party owner's limits only","AVERAGE: use the average of all members' limits.");
	BooleanValue REQUIRE_GAME_STAGE = CONFIG.getBoolean("require_game_stage", false).comment("If true, the player must have the 'ftbchunks_mapping' Game stage to be able to use the map and minimap.\nRequires KubeJS and/or Gamestages to be installed.");
	BooleanValue LOCATION_MODE_OVERRIDE = CONFIG.getBoolean("location_mode_override", false).comment("If true, \"Location Visibility\" team settings are ignored, and all players can see each other anywhere on the map.");
	BooleanValue OFFLINE_PROTECTION_ONLY_ALL = CONFIG.getBoolean("offline_protection_only_all", false).comment("If enabled and disable_protection = false all protections will be disabled when the team is online.");
	BooleanValue OFFLINE_PROTECTION_ONLY_BLOCKS = CONFIG.getBoolean("offline_protection_only_blocks", false).comment("If enabled and disable_protection = false block protection ONLY will be disabled when a team is online.");
	IntValue OFFLINE_PROTECTION_BUFFER = CONFIG.getInt("offline_protection_buffer", 30).comment("If offline_protection_only = true, the time in SECONDS after all members of a team log off, before chunk protection turns on. This setting is meant to discourage combat logging. Set to 0 to disable. Set to -1 for unlimited block breaking if offline_protection_only = true.");
	IntValue MAX_DESTROY_BLOCKS_PER_HOUR = CONFIG.getInt("max_destroy_blocks_per_hour", 0).comment("If disable_protection = false, this many blocks can still be destroyed per hour be enemy players. 0 disables this.");
	IntValue DESTROY_BLOCKS_COUNT_PERIOD = CONFIG.getInt("destroy_blocks_count_period", 300).comment("If max_destroy_blocks_per_hour > 0, the groups of time in seconds where the number of blocks broken are counted. Groups younger than an hour contribute to the total blocks broken. Groups older than an hour are removed.");
	BooleanValue PROTECT_ENTITIES_OFFLINE_ONLY = CONFIG.getBoolean("protect_entities_offline_only", true).comment("Only protect the living entities listed in the living_entity_attack_blacklist tag when all team members are offline.");
	BooleanValue PROTECT_NAMED_ENTITIES = CONFIG.getBoolean("protect_named_entities", false).comment("Protect entities that have a name tag.");
	BooleanValue PROTECT_UNKNOWN_BLOCK_BREAKER = CONFIG.getBoolean("protect_unknown_block_breaker", true).comment("Protect blocks if the owner of the block breaker is not defined.");
	BooleanValue ALLOW_UNKNOWN_BREAKS_IN_FORCE_LOADS = CONFIG.getBoolean("allow_unknown_breaks_in_force_loads", true).comment("Allow blocks to be broken by unknown (null) players in force loaded chunks.");
	BooleanValue ALLOW_EXPLODE_BREAK_COUNT = CONFIG.getBoolean("allow_explode_break_count", false).comment("Allow blocks to be broken by known explosions and contribute to max_destroy_blocks_per_hour.");
	//BooleanValue DISABLE_PROTECTION_IN_COMBAT = CONFIG.getBoolean("disable_protection_in_combat", false).comment("If enabled, protection will be removed if players from both factions attack each other.");
	BooleanValue OPS_SEE_ALL_PLAYERS_MAP = CONFIG.getBoolean("ops_see_all_players_map", false).comment("Allow ops to see all nearby players on the minimap.");
	//fix the block break black list

	static int getMaxClaimedChunks(FTBChunksTeamData playerData, ServerPlayer player) {
		if (player != null) {
			return PermissionsHelper.getMaxClaimedChunks(player, MAX_CLAIMED_CHUNKS.get()) + playerData.getExtraClaimChunks();
		}

		return MAX_CLAIMED_CHUNKS.get() + playerData.getExtraClaimChunks();
	}

	static int getMaxForceLoadedChunks(FTBChunksTeamData playerData, ServerPlayer player) {
		if (player != null) {
			return PermissionsHelper.getMaxForceLoadedChunks(player, MAX_FORCE_LOADED_CHUNKS.get()) + playerData.getExtraForceLoadChunks();
		}

		return MAX_FORCE_LOADED_CHUNKS.get() + playerData.getExtraForceLoadChunks();
	}

	static boolean canPlayerOfflineForceload(ServerPlayer player) {
		// note: purely checking the player's own permission here; not interested in server defaults or party data
		return player != null && PermissionsHelper.getChunkLoadOffline(player, false);
	}

	static boolean noWilderness(ServerPlayer player) {
		if (player != null) {
			return DimensionFilter.isNoWildernessDimension(player.level.dimension())
					|| PermissionsHelper.getNoWilderness(player, NO_WILDERNESS.get());
		}

		return NO_WILDERNESS.get();
	}

	static boolean playerHasMapStage(Player player) {
		return !REQUIRE_GAME_STAGE.get() || StageHelper.getInstance().getProvider().has(player, "ftbchunks_mapping");
	}

	static boolean shouldShowMinimap(Player player) {
		return !FORCE_DISABLE_MINIMAP.get() && playerHasMapStage(player);
	}

}