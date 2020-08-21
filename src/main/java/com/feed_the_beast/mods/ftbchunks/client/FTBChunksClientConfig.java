package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.client.map.MapManager;
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
	public static float noise;
	public static float shadows;
	public static boolean chunkGrid;
	public static boolean reducedColorPalette;
	public static float saturation;
	public static boolean claimedChunksOnMap;
	public static boolean ownClaimedChunksOnMap;
	public static boolean inWorldWaypoints;
	public static boolean topographyMode;

	public static MinimapPosition minimap;
	public static double minimapScale;
	public static boolean minimapLockedNorth;
	public static boolean minimapWaypoints;
	public static boolean minimapPlayerHeads;
	public static boolean minimapEntities;
	public static boolean minimapEntityHeads;
	public static boolean minimapLargeEntities;
	public static boolean minimapXYZ;
	public static boolean minimapBiome;
	public static boolean minimapBlur;
	public static boolean minimapCompass;
	public static int minimapVisibility;
	public static boolean minimapZone;

	public static boolean debugInfo;
	public static int taskQueueTicks = 4;
	public static int taskQueueMin = 20;
	public static int taskQueueMax = 100;

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
			noise = c.noise.get().floatValue();
			shadows = c.shadows.get().floatValue();
			chunkGrid = c.chunkGrid.get();
			reducedColorPalette = c.reducedColorPalette.get();
			saturation = c.saturation.get().floatValue();
			claimedChunksOnMap = c.claimedChunksOnMap.get();
			ownClaimedChunksOnMap = c.ownClaimedChunksOnMap.get();
			inWorldWaypoints = c.inWorldWaypoints.get();
			topographyMode = c.topographyMode.get();

			minimap = c.minimap.get();
			minimapScale = c.minimapScale.get();
			minimapLockedNorth = c.minimapLockedNorth.get();
			minimapWaypoints = c.minimapWaypoints.get();
			minimapPlayerHeads = c.minimapPlayerHeads.get();
			minimapEntities = c.minimapEntities.get();
			minimapEntityHeads = c.minimapEntityHeads.get();
			minimapLargeEntities = c.minimapLargeEntities.get();
			minimapXYZ = c.minimapXYZ.get();
			minimapBiome = c.minimapBiome.get();
			minimapBlur = c.minimapBlur.get();
			minimapCompass = c.minimapCompass.get();
			minimapVisibility = c.minimapVisibility.get();
			minimapZone = c.minimapZone.get();

			debugInfo = c.debugInfo.get();

			if (MapManager.inst != null)
			{
				MapManager.inst.updateAllRegions(false);
			}
		}
	}

	private static class ClientConfig
	{
		public final ForgeConfigSpec.DoubleValue noise;
		public final ForgeConfigSpec.DoubleValue shadows;
		public final ForgeConfigSpec.BooleanValue chunkGrid;
		public final ForgeConfigSpec.BooleanValue reducedColorPalette;
		public final ForgeConfigSpec.DoubleValue saturation;
		public final ForgeConfigSpec.BooleanValue claimedChunksOnMap;
		public final ForgeConfigSpec.BooleanValue ownClaimedChunksOnMap;
		public final ForgeConfigSpec.BooleanValue inWorldWaypoints;
		public final ForgeConfigSpec.BooleanValue topographyMode;

		private final ForgeConfigSpec.EnumValue<MinimapPosition> minimap;
		public final ForgeConfigSpec.DoubleValue minimapScale;
		public final ForgeConfigSpec.BooleanValue minimapLockedNorth;
		public final ForgeConfigSpec.BooleanValue minimapWaypoints;
		public final ForgeConfigSpec.BooleanValue minimapEntities;
		public final ForgeConfigSpec.BooleanValue minimapEntityHeads;
		public final ForgeConfigSpec.BooleanValue minimapPlayerHeads;
		public final ForgeConfigSpec.BooleanValue minimapLargeEntities;
		public final ForgeConfigSpec.BooleanValue minimapXYZ;
		public final ForgeConfigSpec.BooleanValue minimapBiome;
		public final ForgeConfigSpec.BooleanValue minimapBlur;
		public final ForgeConfigSpec.BooleanValue minimapCompass;
		public final ForgeConfigSpec.IntValue minimapVisibility;
		public final ForgeConfigSpec.BooleanValue minimapZone;

		public final ForgeConfigSpec.BooleanValue debugInfo;

		private ClientConfig(ForgeConfigSpec.Builder builder)
		{
			noise = builder
					.comment("Noise added to map to make it look less plastic")
					.translation("ftbchunks.noise")
					.defineInRange("noise", 0.05D, 0D, 0.5D);

			shadows = builder
					.comment("Shadow intensity")
					.translation("ftbchunks.shadows")
					.defineInRange("shadows", 0.1D, 0D, 0.3D);

			chunkGrid = builder
					.comment("Chunk grid overlay in large map")
					.translation("ftbchunks.chunk_grid")
					.define("chunk_grid", false);

			reducedColorPalette = builder
					.comment("Reduces color palette to 256 colors")
					.translation("ftbchunks.reduced_color_palette")
					.define("reduced_color_palette", false);

			saturation = builder
					.comment("Color intensity")
					.translation("ftbchunks.saturation")
					.defineInRange("saturation", 1D, 0D, 1D);

			claimedChunksOnMap = builder
					.comment("Show claimed chunks on the map")
					.translation("ftbchunks.claimed_chunks_on_map")
					.define("claimed_chunks_on_map", true);

			ownClaimedChunksOnMap = builder
					.comment("Show your own claimed chunks on the map")
					.translation("ftbchunks.own_claimed_chunks_on_map")
					.define("own_claimed_chunks_on_map", true);

			inWorldWaypoints = builder
					.comment("Show waypoints in world")
					.translation("ftbchunks.in_world_waypoints")
					.define("in_world_waypoints", true);

			topographyMode = builder
					.comment("Show waypoints in world")
					.translation("ftbchunks.topography_mode")
					.define("topography_mode", false);

			minimap = builder
					.comment("Enables minimap to show up in corner")
					.translation("ftbchunks.minimap")
					.defineEnum("minimap", MinimapPosition.TOP_RIGHT);

			minimapScale = builder
					.comment("Scale of minimap")
					.translation("ftbchunks.minimap_scale")
					.defineInRange("minimap_scale", 1D, 0.25D, 4D);

			minimapLockedNorth = builder
					.comment("Minimap will not rotate")
					.translation("ftbchunks.minimap_locked_north")
					.define("minimap_locked_north", true);

			minimapWaypoints = builder
					.comment("Show waypoints on minimap")
					.translation("ftbchunks.minimap_waypoints")
					.define("minimap_waypoints", true);

			minimapEntities = builder
					.comment("Show entities on minimap")
					.translation("ftbchunks.minimap_entities")
					.define("minimap_entities", true);

			minimapEntityHeads = builder
					.comment("Show entity heads on minimap")
					.translation("ftbchunks.minimap_entity_heads")
					.define("minimap_entity_heads", true);

			minimapPlayerHeads = builder
					.comment("Show player heads on minimap")
					.translation("ftbchunks.minimap_player_heads")
					.define("minimap_player_heads", true);

			minimapLargeEntities = builder
					.comment("Entities in minimap will be larger")
					.translation("ftbchunks.minimap_large_entities")
					.define("minimap_large_entities", false);

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

			minimapCompass = builder
					.comment("Adds NWSE compass inside minimap")
					.translation("ftbchunks.minimap_compass")
					.define("minimap_compass", true);

			minimapVisibility = builder
					.comment("Minimap visibility")
					.translation("ftbchunks.minimap_visibility")
					.defineInRange("minimap_visibility", 255, 0, 255);

			minimapZone = builder
					.comment("Show zone (claimed chunk or wilderness) under minimap")
					.translation("ftbchunks.minimap_zone")
					.define("minimap_zone", true);

			debugInfo = builder
					.comment("Enables debug info")
					.translation("ftbchunks.debug_info")
					.define("debug_info", false);
		}
	}

	public static void openSettings()
	{
		ConfigGroup group = new ConfigGroup("ftbchunks");

		group.addDouble("noise", noise, v -> {
			client.getLeft().noise.set(v);
			noise = v.floatValue();
		}, 0.05D, 0D, 0.5D);

		group.addDouble("shadows", shadows, v -> {
			client.getLeft().shadows.set(v);
			shadows = v.floatValue();
		}, 0.1D, 0D, 0.3D);

		group.addBool("chunk_grid", chunkGrid, v -> {
			client.getLeft().chunkGrid.set(v);
			chunkGrid = v;
		}, false);

		group.addBool("reduced_color_palette", reducedColorPalette, v -> {
			client.getLeft().reducedColorPalette.set(v);
			reducedColorPalette = v;
		}, false);

		group.addDouble("saturation", saturation, v -> {
			client.getLeft().saturation.set(v);
			saturation = v.floatValue();
		}, 1D, 0D, 1D);

		group.addBool("claimed_chunks_on_map", claimedChunksOnMap, v -> {
			client.getLeft().claimedChunksOnMap.set(v);
			claimedChunksOnMap = v;
		}, true);

		group.addBool("own_claimed_chunks_on_map", ownClaimedChunksOnMap, v -> {
			client.getLeft().ownClaimedChunksOnMap.set(v);
			ownClaimedChunksOnMap = v;
		}, true);

		group.addBool("in_world_waypoints", inWorldWaypoints, v -> {
			client.getLeft().inWorldWaypoints.set(v);
			inWorldWaypoints = v;
		}, true);

		group.addBool("topography_mode", topographyMode, v -> {
			client.getLeft().topographyMode.set(v);
			topographyMode = v;
		}, false);

		group.addEnum("minimap", minimap, v -> {
			client.getLeft().minimap.set(v);
			minimap = v;
		}, NameMap.of(MinimapPosition.TOP_RIGHT, MinimapPosition.values()).create());

		group.addDouble("minimap_scale", minimapScale, v -> {
			client.getLeft().minimapScale.set(v);
			minimapScale = v;
		}, 1D, 0.25D, 4D);

		group.addBool("minimap_locked_north", minimapLockedNorth, v -> {
			client.getLeft().minimapLockedNorth.set(v);
			minimapLockedNorth = v;
		}, true);

		group.addBool("minimap_waypoints", minimapWaypoints, v -> {
			client.getLeft().minimapWaypoints.set(v);
			minimapWaypoints = v;
		}, true);

		group.addBool("minimap_entities", minimapEntities, v -> {
			client.getLeft().minimapEntities.set(v);
			minimapEntities = v;
		}, true);

		group.addBool("minimap_entity_heads", minimapEntityHeads, v -> {
			client.getLeft().minimapEntityHeads.set(v);
			minimapEntityHeads = v;
		}, true);

		group.addBool("minimap_player_heads", minimapPlayerHeads, v -> {
			client.getLeft().minimapPlayerHeads.set(v);
			minimapPlayerHeads = v;
		}, true);

		group.addBool("minimap_large_entities", minimapLargeEntities, v -> {
			client.getLeft().minimapLargeEntities.set(v);
			minimapLargeEntities = v;
		}, false);

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

		group.addBool("minimap_compass", minimapCompass, v -> {
			client.getLeft().minimapCompass.set(v);
			minimapCompass = v;
		}, true);

		group.addInt("minimap_visibility", minimapVisibility, v -> {
			client.getLeft().minimapVisibility.set(v);
			minimapVisibility = v;
		}, 255, 0, 255);

		group.addBool("minimap_zone", minimapZone, v -> {
			client.getLeft().minimapZone.set(v);
			minimapZone = v;
		}, true);

		group.addBool("debug_info", debugInfo, v -> {
			client.getLeft().debugInfo.set(v);
			debugInfo = v;
		}, false);

		GuiEditConfig gui = new GuiEditConfig(group);
		group.savedCallback = b -> {
			if (b)
			{
				client.getRight().save();
			}

			if (MapManager.inst != null)
			{
				MapManager.inst.updateAllRegions(false);
			}

			new LargeMapScreen().openGui();
		};

		gui.openGui();
	}
}
