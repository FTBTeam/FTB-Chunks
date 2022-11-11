package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.data.*;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.snbt.config.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author LatvianModder
 */
public interface FTBChunksWorldConfig {
	SNBTConfig CONFIG = SNBTConfig.create(FTBChunks.MOD_ID + "-world");

	EnumValue<ProtectionOverride> FAKE_PLAYERS = CONFIG.getEnum("fake_players", NameMap.of(ProtectionOverride.CHECK, ProtectionOverride.values()).create()).comment("Override to disable/enable fake players like miners and auto-clickers globally. Default will check this setting for each team");
	IntValue MAX_CLAIMED_CHUNKS = CONFIG.getInt("max_claimed_chunks", 500).comment("Max claimed chunks.", "You can override this with FTB Ranks 'ftbchunks.max_claimed' permission");
	IntValue MAX_FORCE_LOADED_CHUNKS = CONFIG.getInt("max_force_loaded_chunks", 25).comment("Max force loaded chunks.", "You can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission");
	BooleanValue CHUNK_LOAD_OFFLINE = CONFIG.getBoolean("chunk_load_offline", true).comment("Allow players to force-load chunks while they are offline. This may not take effect until each player re-logs in server, for immediate effect use 'force_load_mode'. You can override this with FTB Ranks 'ftbchunks.chunk_load_offline' permission.");
	EnumValue<ForceLoadMode> FORCE_LOAD_MODE = CONFIG.getEnum("force_load_mode", ForceLoadMode.NAME_MAP).comment("Control how force-loaded chunks work. NEVER: do not allow a team's chunks to be loaded when the team has no online players. ALWAYS: always allow offline chunkloading. DEFAULT: allow offline chunkloading if the team has at least one online player with 'chunk_load_offline' permission.");
	BooleanValue DISABLE_PROTECTION = CONFIG.getBoolean("disable_protection", false).comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for force-loading");
	EnumValue<AllyMode> ALLY_MODE = CONFIG.getEnum("ally_mode", AllyMode.NAME_MAP).comment("Forced modes won't let players change their ally settings");
	StringListValue CLAIM_DIMENSION_BLACKLIST = CONFIG.getStringList("claim_dimension_blacklist", Collections.emptyList()).comment("Blacklist for dimensions where chunks can't be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End");
	BooleanValue NO_WILDERNESS = CONFIG.getBoolean("no_wilderness", false).comment("Requires you to claim chunks in order to edit and interact with blocks");
	BooleanValue FORCE_DISABLE_MINIMAP = CONFIG.getBoolean("force_disable_minimap", false).comment("Minimap for clients connecting to this server will be disabled");
	LongValue MAX_IDLE_DAYS_BEFORE_UNCLAIM = CONFIG.getLong("max_idle_days_before_unclaim", 0L, 0L, Long.MAX_VALUE).comment("Maximum time (in real-world days) where if no player in a team logs in, the team automatically loses their claims. Prevents chunks being claimed indefinitely by teams who no longer play. Default of 0 means no automatic loss of claims.");
	IntValue LONG_RANGE_TRACKER_INTERVAL = CONFIG.getInt("long_range_tracker_interval", 20, 0, Integer.MAX_VALUE).comment("Interval in ticks to send updates to clients with long-range player tracking data. Setting this to 0 disables long-range tracking. Lower values mean more frequent updates but more server load and network traffic; be careful with this, especially on busy servers.");
	BooleanValue PROTECT_UNKNOWN_EXPLOSIONS = CONFIG.getBoolean("protect_unknown_explosions", true).comment("When true, standard FTB Chunk explosion protection is applied in protected chunks when the source of the explosion cannot be determined (Ghast fireballs are a common case - vanilla supplies a null entity source)");

	Set<ResourceKey<Level>> CLAIM_DIMENSION_BLACKLIST_SET = new HashSet<>();

	static int getMaxClaimedChunks(FTBChunksTeamData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod && player != null) {
			return FTBRanksIntegration.getMaxClaimedChunks(player, MAX_CLAIMED_CHUNKS.get()) + playerData.getExtraClaimChunks();
		}

		return MAX_CLAIMED_CHUNKS.get() + playerData.getExtraClaimChunks();
	}

	static int getMaxForceLoadedChunks(FTBChunksTeamData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod && player != null) {
			return FTBRanksIntegration.getMaxForceLoadedChunks(player, MAX_FORCE_LOADED_CHUNKS.get()) + playerData.getExtraForceLoadChunks();
		}

		return MAX_FORCE_LOADED_CHUNKS.get() + playerData.getExtraForceLoadChunks();
	}

	static boolean getChunkLoadOffline(ServerPlayer player) {
		if (FTBChunks.ranksMod && player != null) {
			return FTBRanksIntegration.getChunkLoadOffline(player, CHUNK_LOAD_OFFLINE.get());
		}

		return CHUNK_LOAD_OFFLINE.get();
	}

	static boolean noWilderness(ServerPlayer player) {
		if (FTBChunks.ranksMod && player != null) {
			return FTBRanksIntegration.getNoWilderness(player, NO_WILDERNESS.get());
		}

		return NO_WILDERNESS.get();
	}
}