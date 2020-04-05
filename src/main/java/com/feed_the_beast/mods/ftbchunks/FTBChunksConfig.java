package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbchunks.impl.AllyMode;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author LatvianModder
 */
public class FTBChunksConfig
{
	public static boolean disableAllFakePlayers;
	public static int maxClaimedChunks;
	public static int maxForceLoadedChunks;
	public static AllyMode allyMode;

	private static Pair<ServerConfig, ForgeConfigSpec> server;

	public static void init()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(FTBChunksConfig::reload);

		server = new ForgeConfigSpec.Builder().configure(ServerConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.SERVER, server.getRight());
	}

	public static void reload(ModConfig.ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == server.getRight())
		{
			ServerConfig c = server.getLeft();
			disableAllFakePlayers = c.disableAllFakePlayers.get();
			maxClaimedChunks = c.maxClaimedChunks.get();
			maxForceLoadedChunks = c.maxForceLoadedChunks.get();
			allyMode = c.allyMode.get();
		}
	}

	private static class ServerConfig
	{
		private final ForgeConfigSpec.BooleanValue disableAllFakePlayers;
		private final ForgeConfigSpec.IntValue maxClaimedChunks;
		private final ForgeConfigSpec.IntValue maxForceLoadedChunks;
		private final ForgeConfigSpec.EnumValue<AllyMode> allyMode;

		private ServerConfig(ForgeConfigSpec.Builder builder)
		{
			disableAllFakePlayers = builder
					.comment("Disables fake players like miners and auto-clickers.")
					.translation("ftbchunks.general.disable_fake_players")
					.define("disable_fake_players", false);

			maxClaimedChunks = builder
					.comment("Max claimed chunks.")
					.translation("ftbchunks.general.max_claimed_chunks")
					.defineInRange("max_claimed_chunks", 500, 0, Integer.MAX_VALUE);

			maxForceLoadedChunks = builder
					.comment("Max force loaded chunks.")
					.translation("ftbchunks.general.max_force_loaded_chunks")
					.defineInRange("max_force_loaded_chunks", 25, 0, Integer.MAX_VALUE);

			allyMode = builder
					.comment("Forced modes won't let players change their ally settings.")
					.translation("ftbchunks.general.ally_mode")
					.defineEnum("ally_mode", AllyMode.DEFAULT);
		}
	}

	public static int getMaxClaimedChunks(ServerPlayerEntity player)
	{
		if (FTBChunks.ranksMod)
		{
			return FTBRanksIntegration.getMaxClaimedChunks(player, maxClaimedChunks);
		}

		return maxClaimedChunks;
	}

	public static int getMaxForceLoadedChunks(ServerPlayerEntity player)
	{
		if (FTBChunks.ranksMod)
		{
			return FTBRanksIntegration.getMaxForceLoadedChunks(player, maxClaimedChunks);
		}

		return maxForceLoadedChunks;
	}
}