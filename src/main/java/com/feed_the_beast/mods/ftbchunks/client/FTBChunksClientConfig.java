package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.config.ConfigGroup;
import com.feed_the_beast.mods.ftbguilibrary.config.NameMap;
import com.feed_the_beast.mods.ftbguilibrary.config.gui.GuiEditConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author LatvianModder
 */
public class FTBChunksClientConfig
{
	public static double noise;
	public static MinimapPosition minimap;
	public static double minimapScale;
	public static boolean minimapWaypoints;
	public static boolean minimapPlayerHeads;
	public static boolean minimapXYZ;
	public static boolean minimapBiome;
	public static boolean minimapBlur;

	private static Pair<ClientConfig, ForgeConfigSpec> client;

	public static void init()
	{
		FMLJavaModLoadingContext.get().getModEventBus().register(FTBChunksClientConfig.class);

		client = new ForgeConfigSpec.Builder().configure(ClientConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.CLIENT, client.getRight());
	}

	@SubscribeEvent
	public static void reload(ModConfig.ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == client.getRight())
		{
			ClientConfig c = client.getLeft();
			noise = c.noise.get();
			minimap = c.minimap.get();
			minimapScale = c.minimapScale.get();
			minimapWaypoints = c.minimapWaypoints.get();
			minimapPlayerHeads = c.minimapPlayerHeads.get();
			minimapXYZ = c.minimapXYZ.get();
			minimapBiome = c.minimapBiome.get();
			minimapBlur = c.minimapBlur.get();
		}
	}

	private static class ClientConfig
	{
		public final ForgeConfigSpec.DoubleValue noise;
		private final ForgeConfigSpec.EnumValue<MinimapPosition> minimap;
		public final ForgeConfigSpec.DoubleValue minimapScale;
		public final ForgeConfigSpec.BooleanValue minimapWaypoints;
		public final ForgeConfigSpec.BooleanValue minimapPlayerHeads;
		public final ForgeConfigSpec.BooleanValue minimapXYZ;
		public final ForgeConfigSpec.BooleanValue minimapBiome;
		public final ForgeConfigSpec.BooleanValue minimapBlur;

		private ClientConfig(ForgeConfigSpec.Builder builder)
		{
			noise = builder
					.comment("Noise added to map to make it look less plastic")
					.translation("ftbchunks.noise")
					.defineInRange("noise", 0.05D, 0D, 0.5D);

			minimap = builder
					.comment("Enables minimap to show up in corner")
					.translation("ftbchunks.minimap")
					.defineEnum("minimap", MinimapPosition.TOP_RIGHT);

			minimapScale = builder
					.comment("Scale of minimap")
					.translation("ftbchunks.minimap_scale")
					.defineInRange("minimap_scale", 1D, 0.25D, 4D);

			minimapWaypoints = builder
					.comment("Show waypoints on minimap")
					.translation("ftbchunks.minimap_waypoints")
					.define("minimap_waypoints", true);

			minimapPlayerHeads = builder
					.comment("Show player heads on minimap")
					.translation("ftbchunks.minimap_player_heads")
					.define("minimap_player_heads", true);

			minimapXYZ = builder
					.comment("Show XYZ under minimap")
					.translation("ftbchunks.minimap_xyz")
					.define("minimap_xyz", true);

			minimapBiome = builder
					.comment("Show biome under minimap")
					.translation("ftbchunks.minimap_biome")
					.define("minimap_biome", true);

			minimapBlur = builder
					.comment("Blurs minimap")
					.translation("ftbchunks.minimap_blur")
					.define("minimap_blur", true);
		}
	}

	public static void openSettings()
	{
		ConfigGroup group = new ConfigGroup("ftbchunks");

		group.addDouble("noise", noise, v -> {
			client.getLeft().noise.set(v);
			noise = v;
		}, 0.05D, 0D, 0.5D);

		group.addEnum("minimap", minimap, v -> {
			client.getLeft().minimap.set(v);
			minimap = v;
		}, NameMap.of(MinimapPosition.TOP_RIGHT, MinimapPosition.values()).create());

		group.addDouble("minimap_scale", minimapScale, v -> {
			client.getLeft().minimapScale.set(v);
			minimapScale = v;
		}, 1D, 0.25D, 4D);

		group.addBool("minimap_waypoints", minimapWaypoints, v -> {
			client.getLeft().minimapWaypoints.set(v);
			minimapWaypoints = v;
		}, true);

		group.addBool("minimap_player_heads", minimapPlayerHeads, v -> {
			client.getLeft().minimapPlayerHeads.set(v);
			minimapPlayerHeads = v;
		}, true);

		group.addBool("minimap_xyz", minimapXYZ, v -> {
			client.getLeft().minimapXYZ.set(v);
			minimapXYZ = v;
		}, true);

		group.addBool("minimap_biome", minimapBiome, v -> {
			client.getLeft().minimapBiome.set(v);
			minimapBiome = v;
		}, true);

		group.addBool("minimap_blur", minimapBlur, v -> {
			client.getLeft().minimapBlur.set(v);
			minimapBlur = v;
		}, true);

		GuiEditConfig gui = new GuiEditConfig(group);
		group.savedCallback = b -> {
			if (b)
			{
				client.getRight().save();
			}

			gui.closeGui(false);
		};

		gui.openGui();
	}
}
