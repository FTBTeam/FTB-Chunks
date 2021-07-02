package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.data.AllyMode;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.EnumValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import dev.ftb.mods.ftblibrary.snbt.config.StringListValue;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author LatvianModder
 */
public interface FTBChunksWorldConfig {
	LevelResource CONFIG_FILE_PATH = LevelResourceHooks.create("serverconfig/ftbchunks.snbt");
	SNBTConfig CONFIG = SNBTConfig.create(FTBChunks.MOD_ID + "-world");

	BooleanValue DISABLE_ALL_FAKE_PLAYERS = CONFIG.getBoolean("disable_all_fake_players", false).comment("Disables fake players like miners and auto-clickers");
	IntValue MAX_CLAIMED_CHUNKS = CONFIG.getInt("max_claimed_chunks", 500).comment("Max claimed chunks.", "You can override this with FTB Ranks 'ftbchunks.max_claimed' permission");
	IntValue MAX_FORCE_LOADED_CHUNKS = CONFIG.getInt("max_force_loaded_chunks", 25).comment("Max force loaded chunks.", "You can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission");
	BooleanValue CHUNK_LOAD_OFFLINE = CONFIG.getBoolean("chunk_load_offline", true).comment("Allow players to load chunks while they are offline");
	BooleanValue DISABLE_PROTECTION = CONFIG.getBoolean("disable_protection", false).comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for forceloading");
	EnumValue<AllyMode> ALLY_MODE = CONFIG.getEnum("ally_mode", AllyMode.NAME_MAP).comment("Forced modes won't let players change their ally settings");
	StringListValue CLAIM_DIMENSION_BLACKLIST = CONFIG.getStringList("claim_dimension_blacklist", Collections.emptyList()).comment("Blacklist for dimensions where chunks can't be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End");
	BooleanValue PATCH_CHUNK_LOADING = CONFIG.getBoolean("patch_chunk_loading", true).comment("Patches vanilla chunkloading to allow random block ticks and other environment updates in chunks where no players are nearby. With this off farms and other things won't work. Disable in case this causes issues");
	BooleanValue NO_WILDERNESS = CONFIG.getBoolean("no_wilderness", false).comment("Requires you to claim chunks in order to edit and interact with blocks");

	Set<ResourceKey<Level>> CLAIM_DIMENSION_BLACKLIST_SET = new HashSet<>();

	static int getMaxClaimedChunks(FTBChunksTeamData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod) {
			return FTBRanksIntegration.getMaxClaimedChunks(player, MAX_CLAIMED_CHUNKS.get()) + playerData.getExtraClaimChunks();
		}

		return MAX_CLAIMED_CHUNKS.get() + playerData.getExtraClaimChunks();
	}

	static int getMaxForceLoadedChunks(FTBChunksTeamData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod) {
			return FTBRanksIntegration.getMaxForceLoadedChunks(player, MAX_FORCE_LOADED_CHUNKS.get()) + playerData.getExtraForceLoadChunks();
		}

		return MAX_FORCE_LOADED_CHUNKS.get() + playerData.getExtraForceLoadChunks();
	}

	static boolean getChunkLoadOffline(FTBChunksTeamData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod) {
			return FTBRanksIntegration.getChunkLoadOffline(player, CHUNK_LOAD_OFFLINE.get());
		}

		return CHUNK_LOAD_OFFLINE.get();
	}

	static boolean patchChunkLoading(ServerLevel world, ChunkPos pos) {
		if (PATCH_CHUNK_LOADING.get()) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(world.dimension(), pos));
			return chunk != null && chunk.isForceLoaded();
		}

		return false;
	}
}