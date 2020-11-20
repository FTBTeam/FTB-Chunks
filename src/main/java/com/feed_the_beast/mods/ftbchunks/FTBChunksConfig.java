package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkPlayerData;
import com.feed_the_beast.mods.ftbchunks.impl.AllyMode;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class FTBChunksConfig
{
	public static boolean disableAllFakePlayers;
	public static int maxClaimedChunks;
	public static int maxForceLoadedChunks;
	public static boolean disableProtection;
	public static AllyMode allyMode;
	public static Set<RegistryKey<World>> claimDimensionBlacklist;
	public static boolean patchChunkLoading;

	private static Pair<ServerConfig, ForgeConfigSpec> server;

	public static void init()
	{
		FMLJavaModLoadingContext.get().getModEventBus().register(FTBChunksConfig.class);

		server = new ForgeConfigSpec.Builder().configure(ServerConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.SERVER, server.getRight());
	}

	@SubscribeEvent
	public static void reload(ModConfig.ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == server.getRight())
		{
			ServerConfig c = server.getLeft();
			disableAllFakePlayers = c.disableAllFakePlayers.get();
			maxClaimedChunks = c.maxClaimedChunks.get();
			maxForceLoadedChunks = c.maxForceLoadedChunks.get();
			disableProtection = c.disableProtection.get();
			allyMode = c.allyMode.get();
			claimDimensionBlacklist = new HashSet<>();

			for (String s : c.claimDimensionBlacklist.get())
			{
				claimDimensionBlacklist.add(RegistryKey.getOrCreateKey(Registry.WORLD_KEY, new ResourceLocation(s)));
			}

			patchChunkLoading = c.patchChunkLoading.get();
		}
	}

	private static class ServerConfig
	{
		private final ForgeConfigSpec.BooleanValue disableAllFakePlayers;
		private final ForgeConfigSpec.IntValue maxClaimedChunks;
		private final ForgeConfigSpec.IntValue maxForceLoadedChunks;
		private final ForgeConfigSpec.BooleanValue disableProtection;
		private final ForgeConfigSpec.EnumValue<AllyMode> allyMode;
		private final ForgeConfigSpec.ConfigValue<List<? extends String>> claimDimensionBlacklist;
		private final ForgeConfigSpec.BooleanValue patchChunkLoading;

		private ServerConfig(ForgeConfigSpec.Builder builder)
		{
			disableAllFakePlayers = builder
					.comment("Disables fake players like miners and auto-clickers.")
					.define("disable_fake_players", false);

			maxClaimedChunks = builder
					.comment("Max claimed chunks.", "You can override this with FTB Ranks 'ftbchunks.max_claimed' permission")
					.defineInRange("max_claimed_chunks", 500, 0, Integer.MAX_VALUE);

			maxForceLoadedChunks = builder
					.comment("Max force loaded chunks.", "You can override this with FTB Ranks 'ftbchunks.max_force_loaded' permission")
					.defineInRange("max_force_loaded_chunks", 25, 0, Integer.MAX_VALUE);

			disableProtection = builder
					.comment("Disables all land protection. Useful for private servers where everyone is trusted and claims are only used for forceloading.")
					.define("disable_protection", false);

			allyMode = builder
					.comment("Forced modes won't let players change their ally settings.")
					.defineEnum("ally_mode", AllyMode.DEFAULT);

			claimDimensionBlacklist = builder
					.comment("Blacklist for dimensions where chunks can't be claimed.")
					.defineList("claim_dimension_blacklist", Util.make(new ArrayList<>(), l -> l.add("minecraft:the_end")), o -> true);

			patchChunkLoading = builder
					.comment("Patches vanilla chunkloading to allow random block ticks and other environment updates in chunks where no players are nearby. With this off farms and other things won't work. Disable in case this causes issues.")
					.define("patch_chunkloading", true);
		}
	}

	public static int getMaxClaimedChunks(ClaimedChunkPlayerData playerData, ServerPlayerEntity player)
	{
		if (FTBChunks.ranksMod)
		{
			return FTBRanksIntegration.getMaxClaimedChunks(player, maxClaimedChunks) + playerData.getExtraClaimChunks();
		}

		return maxClaimedChunks + playerData.getExtraClaimChunks();
	}

	public static int getMaxForceLoadedChunks(ClaimedChunkPlayerData playerData, ServerPlayerEntity player)
	{
		if (FTBChunks.ranksMod)
		{
			return FTBRanksIntegration.getMaxForceLoadedChunks(player, maxForceLoadedChunks) + playerData.getExtraForceLoadChunks();
		}

		return maxForceLoadedChunks + playerData.getExtraForceLoadChunks();
	}

	public static boolean patchChunkLoading(ServerWorld world, ChunkPos pos)
	{
		if (patchChunkLoading)
		{
			return true;
		}

		return false;
	}
}
