package com.feed_the_beast.mods.ftbchunks;

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
	public boolean disableAllFakePlayers;
	public int maxClaimedChunks;
	public int maxForceLoadedChunks;

	private Pair<ServerConfig, ForgeConfigSpec> server;

	public FTBChunksConfig()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::reload);

		server = new ForgeConfigSpec.Builder().configure(ServerConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.SERVER, server.getRight());
	}

	public void reload(ModConfig.ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == server.getRight())
		{
			ServerConfig c = server.getLeft();
			disableAllFakePlayers = c.disableAllFakePlayers.get();
			maxClaimedChunks = c.maxClaimedChunks.get();
			maxForceLoadedChunks = c.maxForceLoadedChunks.get();
		}
	}

	private static class ServerConfig
	{
		private final ForgeConfigSpec.BooleanValue disableAllFakePlayers;
		private final ForgeConfigSpec.IntValue maxClaimedChunks;
		private final ForgeConfigSpec.IntValue maxForceLoadedChunks;

		private ServerConfig(ForgeConfigSpec.Builder builder)
		{
			disableAllFakePlayers = builder
					.translation("ftbchunks.general.disable_fake_players")
					.define("disable_fake_players", false);

			maxClaimedChunks = builder
					.comment(
							"Max claimed chunks.",
							"-1 - Up to permissions / default"
					)
					.translation("ftbchunks.general.max_claimed_chunks")
					.defineInRange("max_claimed_chunks", 100, -1, Integer.MAX_VALUE);

			maxForceLoadedChunks = builder
					.comment(
							"Max force loaded chunks.",
							"-1 - Up to permissions / default"
					)
					.translation("ftbchunks.general.max_force_loaded_chunks")
					.defineInRange("max_force_loaded_chunks", 25, -1, Integer.MAX_VALUE);
		}
	}
}