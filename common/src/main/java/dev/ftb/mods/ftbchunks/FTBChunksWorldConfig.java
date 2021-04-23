package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.data.AllyMode;
import dev.ftb.mods.ftbchunks.data.ChunkDimPos;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkPlayerData;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import me.shedaniel.architectury.hooks.LevelResourceHooks;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Jankson;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class FTBChunksWorldConfig {
	public static final LevelResource CONFIG_FILE_PATH = LevelResourceHooks.create("serverconfig/ftbchunks-server.json5");

	@Comment("Disables fake players like miners and auto-clickers")
	public boolean disableAllFakePlayers = false;

	@Comment("Max claimed chunks.\nYou can override this with FTB Ranks 'ftbchunks.max_claimed' permission")
	public int maxClaimedChunks = 500;

	@Comment("Max force loaded chunks.\nYou can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission")
	public int maxForceLoadedChunks = 25;

	@Comment("Allow players to load chunks while they are offline")
	public boolean chunkLoadOffline = true;

	@Comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for forceloading")
	public boolean disableProtection = false;

	@Comment("Forced modes won't let players change their ally settings")
	public AllyMode allyMode = AllyMode.DEFAULT;

	@Comment("Blacklist for dimensions where chunks can't be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End")
	public List<String> claimDimensionBlacklist = new ArrayList<>();

	public transient Set<ResourceKey<Level>> claimDimensionBlacklistSet = new HashSet<>();

	@Comment("Patches vanilla chunkloading to allow random block ticks and other environment updates in chunks where no players are nearby. With this off farms and other things won't work. Disable in case this causes issues")
	public boolean patchChunkLoading = true;

	@Comment("Requires you to claim chunks in order to edit and interact with blocks")
	public boolean noWilderness = false;

	/*
	private static Pair<ServerConfig, ForgeConfigSpec> server;

	public static void init() {
		FMLJavaModLoadingContext.get().getModEventBus().register(FTBChunksConfig.class);

		server = new ForgeConfigSpec.Builder().configure(ServerConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.SERVER, server.getRight());
	}

	@SubscribeEvent
	public static void reload(ModConfig.ModConfigEvent event) {
		ModConfig config = event.getConfig();

		if (config.getSpec() == server.getRight()) {
			ServerConfig c = server.getLeft();
			disableAllFakePlayers = c.disableAllFakePlayers.get();
			maxClaimedChunks = c.maxClaimedChunks.get();
			maxForceLoadedChunks = c.maxForceLoadedChunks.get();
			chunkLoadOffline = c.chunkLoadOffline.get();
			disableProtection = c.disableProtection.get();
			allyMode = c.allyMode.get();
			claimDimensionBlacklist = new HashSet<>();

			for (String s : c.claimDimensionBlacklist.get()) {
				claimDimensionBlacklist.add(ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(s)));
			}

			patchChunkLoading = c.patchChunkLoading.get();
			noWilderness = c.noWilderness.get();
		}
	}

	private static class ServerConfig {
		private final ForgeConfigSpec.BooleanValue disableAllFakePlayers;
		private final ForgeConfigSpec.IntValue maxClaimedChunks;
		private final ForgeConfigSpec.IntValue maxForceLoadedChunks;
		private final ForgeConfigSpec.BooleanValue chunkLoadOffline;
		private final ForgeConfigSpec.BooleanValue disableProtection;
		private final ForgeConfigSpec.EnumValue<AllyMode> allyMode;
		private final ForgeConfigSpec.ConfigValue<List<? extends String>> claimDimensionBlacklist;
		private final ForgeConfigSpec.BooleanValue patchChunkLoading;
		private final ForgeConfigSpec.BooleanValue noWilderness;

		private ServerConfig(ForgeConfigSpec.Builder builder) {
			disableAllFakePlayers = builder
					.comment("Disables fake players like miners and auto-clickers.")
					.define("disable_fake_players", false);

			maxClaimedChunks = builder
					.comment("Max claimed chunks.", "You can override this with FTB Ranks 'ftbchunks.max_claimed' permission")
					.defineInRange("max_claimed_chunks", 500, 0, Integer.MAX_VALUE);

			maxForceLoadedChunks = builder
					.comment("Max force loaded chunks.", "You can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission")
					.defineInRange("max_force_loaded_chunks", 25, 0, Integer.MAX_VALUE);

			chunkLoadOffline = builder
					.comment("Allow players to load chunks while they are offline.")
					.define("chunk_load_offline", true);

			disableProtection = builder
					.comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for forceloading.")
					.define("disable_protection", false);

			allyMode = builder
					.comment("Forced modes won't let players change their ally settings.")
					.defineEnum("ally_mode", AllyMode.DEFAULT);

			claimDimensionBlacklist = builder
					.comment("Blacklist for dimensions where chunks can't be claimed. Add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End.")
					.defineList("claim_dimension_blacklist", new ArrayList<>(), o -> true);

			patchChunkLoading = builder
					.comment("Patches vanilla chunkloading to allow random block ticks and other environment updates in chunks where no players are nearby. With this off farms and other things won't work. Disable in case this causes issues.")
					.define("patch_chunkloading", true);

			noWilderness = builder
					.comment("Requires you to claim chunks in order to edit and interact with blocks.")
					.define("no_wilderness", false);
		}
	}
	 */

	private static final Jankson JANKSON = Jankson.builder().build();

	public static FTBChunksWorldConfig init(MinecraftServer server) {
		FTBChunksWorldConfig instance = new FTBChunksWorldConfig();

		Path configPath = server.getWorldPath(CONFIG_FILE_PATH);
		if (Files.exists(configPath)) {
			try {
				instance = JANKSON.fromJson(JANKSON.load(configPath.toFile()), FTBChunksWorldConfig.class);
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}

		for (String s : instance.claimDimensionBlacklist) {
			instance.claimDimensionBlacklistSet.add(ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(s)));
		}

		instance.save(server);
		return instance;
	}

	public void save(MinecraftServer server) {
		Path configPath = server.getWorldPath(CONFIG_FILE_PATH);
		try {
			Files.createDirectories(configPath.getParent());
			BufferedWriter writer = Files.newBufferedWriter(configPath);
			writer.write(JANKSON.toJson(this).toJson(true, true));
			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public int getMaxClaimedChunks(ClaimedChunkPlayerData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod) {
			return FTBRanksIntegration.getMaxClaimedChunks(player, maxClaimedChunks) + playerData.getExtraClaimChunks();
		}

		return maxClaimedChunks + playerData.getExtraClaimChunks();
	}

	public int getMaxForceLoadedChunks(ClaimedChunkPlayerData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod) {
			return FTBRanksIntegration.getMaxForceLoadedChunks(player, maxForceLoadedChunks) + playerData.getExtraForceLoadChunks();
		}

		return maxForceLoadedChunks + playerData.getExtraForceLoadChunks();
	}

	public boolean getChunkLoadOffline(ClaimedChunkPlayerData playerData, ServerPlayer player) {
		if (FTBChunks.ranksMod) {
			return FTBRanksIntegration.getChunkLoadOffline(player, chunkLoadOffline);
		}

		return chunkLoadOffline;
	}

	public boolean patchChunkLoading(ServerLevel world, ChunkPos pos) {
		if (patchChunkLoading) {
			ClaimedChunk chunk = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(world.dimension(), pos));
			return chunk != null && chunk.isForceLoaded();
		}

		return false;
	}
}