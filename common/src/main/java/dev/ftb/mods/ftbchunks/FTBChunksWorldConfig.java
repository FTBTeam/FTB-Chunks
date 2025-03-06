package dev.ftb.mods.ftbchunks;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.ProtectionPolicy;
import dev.ftb.mods.ftbchunks.data.AllyMode;
import dev.ftb.mods.ftbchunks.data.ForceLoadMode;
import dev.ftb.mods.ftbchunks.data.PartyLimitMode;
import dev.ftb.mods.ftbchunks.data.PvPMode;
import dev.ftb.mods.ftbchunks.integration.PermissionsHelper;
import dev.ftb.mods.ftbchunks.util.DimensionFilter;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.integration.stages.StageHelper;
import dev.ftb.mods.ftblibrary.snbt.config.*;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
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
	EnumValue<PvPMode> PVP_MODE = CONFIG.addEnum("pvp_mode", PvPMode.NAME_MAP).comment("Should PvP combat be allowed in claimed chunks? Default is ALWAYS; NEVER prevents it in all claimed chunks; PER_TEAM allows teams to decide if PvP is allowed in their claims");
	StringListValue CLAIM_DIMENSION_BLACKLIST = CONFIG.addStringList("claim_dimension_blacklist", Collections.emptyList()).comment("Dimension ID's where chunks may not be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End, or \"othermod:*\" to disable chunk claiming in *all* dimensions added by \"othermod\"");
	StringListValue CLAIM_DIMENSION_WHITELIST = CONFIG.addStringList("claim_dimension_whitelist", Collections.emptyList()).comment("Dimension ID's where chunks may be claimed. If non-empty, chunks may be claimed *only* in these dimensions (and the dimension is not in \"claim_dimension_blacklist\"). Same syntax as for \"claim_dimension_blacklist\".");
	BooleanValue NO_WILDERNESS = CONFIG.addBoolean("no_wilderness", false).comment("Requires you to claim chunks in order to edit and interact with blocks");
	StringListValue NO_WILDERNESS_DIMENSIONS = CONFIG.addStringList("no_wilderness_dimensions", Collections.emptyList()).comment("Dimension ID's where the no_wilderness rule is enforced - building is only allowed in claimed chunks. If this is non-empty, it overrides the 'no_wilderness' setting.");
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
	IntValue MAX_PREVENTED_LOG_AGE = CONFIG.addInt("max_prevented_log_age", 7, 1, Integer.MAX_VALUE).comment("Maximum time in days to keep logs of prevented fakeplayer access to a team's claims.");
	BooleanValue PISTON_PROTECTION = CONFIG.addBoolean("piston_protection", true)
			.comment("If true, pistons are prevented from pushing/pulling blocks across claims owned by different teams (unless the target claim has public 'edit block' permissions defined). If 'disable_protection' is set to true, this setting is ignored.");

	SNBTConfig WAYPOINT_SHARING = CONFIG.addGroup("waypoint_sharing");
	BooleanValue WAYPOINT_SHARING_SERVER = WAYPOINT_SHARING.addBoolean("waypoint_sharing_server", true).comment("Allow players to share waypoints with the entire server.");
	BooleanValue WAYPOINT_SHARING_PARTY = WAYPOINT_SHARING.addBoolean("waypoint_sharing_party", true).comment("Allow players to share waypoints with their party.");
	BooleanValue WAYPOINT_SHARING_PLAYERS = WAYPOINT_SHARING.addBoolean("waypoint_sharing_players", true).comment("Allow players to share waypoints with other players.");

	SNBTConfig TEAM_PROP_DEFAULTS = CONFIG.addGroup("team_prop_defaults");
	BooleanValue DEF_ALLOW_FAKE_PLAYERS = TEAM_PROP_DEFAULTS.addBoolean("def_fake_players", false)
			.comment("Default allow-fake-player setting for team properties");
	BooleanValue DEF_ALLOW_FAKE_PLAYER_IDS = TEAM_PROP_DEFAULTS.addBoolean("def_fake_player_ids", true)
			.comment("Default allow fake player IDs which are the same as real permitted players");
	StringListValue DEF_ALLOW_NAMED_FAKE_PLAYERS = TEAM_PROP_DEFAULTS.addStringList("def_named_fake_players", Collections.emptyList())
			.comment("Default named fake players who should be allowed by default");
	EnumValue<PrivacyMode> DEF_ENTITY_INTERACT = TEAM_PROP_DEFAULTS.addEnum("def_entity_interact", PrivacyMode.NAME_MAP, PrivacyMode.ALLIES)
			.comment("Default mode for entity interaction in claimed chunks");
	EnumValue<PrivacyMode> DEF_BLOCK_INTERACT = TEAM_PROP_DEFAULTS.addEnum("def_block_interact", PrivacyMode.NAME_MAP, PrivacyMode.ALLIES)
			.comment("Default mode for block interaction (right-click) in claimed chunks (NeoForge only)")
			.enabled(Platform::isForge);
	EnumValue<PrivacyMode> DEF_BLOCK_EDIT = TEAM_PROP_DEFAULTS.addEnum("def_block_edit", PrivacyMode.NAME_MAP, PrivacyMode.ALLIES)
			.comment("Default mode for block breaking and placing in claimed chunks (NeoForge only)")
			.enabled(Platform::isForge);
	EnumValue<PrivacyMode> DEF_BLOCK_EDIT_INTERACT = TEAM_PROP_DEFAULTS.addEnum("def_block_edit_interact", PrivacyMode.NAME_MAP, PrivacyMode.ALLIES)
			.comment("Default mode for block interaction, breaking and placing in claimed chunks (Fabric only)")
			.enabled(Platform::isFabric);
	EnumValue<PrivacyMode> DEF_NONLIVING_ENTITY_ATTACK = TEAM_PROP_DEFAULTS.addEnum("def_entity_attack", PrivacyMode.NAME_MAP, PrivacyMode.ALLIES)
			.comment("Default mode for left-clicking non-living entities (armor stands, item frames...) in claimed chunks");
	BooleanValue DEF_ALLOW_EXPLOSIONS = TEAM_PROP_DEFAULTS.addBoolean("def_allow_explosions", false)
			.comment("Default explosion protection for claimed chunks");
	BooleanValue DEF_MOB_GRIEFING = TEAM_PROP_DEFAULTS.addBoolean("def_mob_griefing", false)
			.comment("Default mob griefing protection for claimed chunks");
	EnumValue<PrivacyMode> DEF_CLAIM_VISIBILITY = TEAM_PROP_DEFAULTS.addEnum("def_claim_visibility", PrivacyMode.NAME_MAP, PrivacyMode.PUBLIC)
			.comment("Default claim visibility for claimed chunks");
	EnumValue<PrivacyMode> DEF_PLAYER_VISIBILITY = TEAM_PROP_DEFAULTS.addEnum("def_player_visibility", PrivacyMode.NAME_MAP, PrivacyMode.ALLIES)
			.comment("Default long-range player visibility on map");
	BooleanValue DEF_PVP = TEAM_PROP_DEFAULTS.addBoolean("def_pvp", true)
			.comment("Default PvP setting in claimed chunks");

	static int getMaxClaimedChunks(ChunkTeamData playerData, ServerPlayer player) {
		if (player != null) {
			return PermissionsHelper.getMaxClaimedChunks(player, MAX_CLAIMED_CHUNKS.get()) + playerData.getExtraClaimChunks();
		}

		return MAX_CLAIMED_CHUNKS.get() + playerData.getExtraClaimChunks();
	}

	static int getMaxForceLoadedChunks(ChunkTeamData playerData, ServerPlayer player) {
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
			return DimensionFilter.isNoWildernessDimension(player.level().dimension())
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