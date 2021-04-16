package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftbguilibrary.config.ConfigGroup;
import dev.ftb.mods.ftbguilibrary.config.NameMap;
import dev.ftb.mods.ftbguilibrary.config.gui.EditConfigScreen;

/**
 * @author LatvianModder
 */
public class FTBChunksClientConfig {
	public static float noise = 0.05F;
	public static float shadows = 0.1F;
	public static boolean chunkGrid = false;
	public static boolean reducedColorPalette = false;
	public static float saturation = 1F;
	public static boolean claimedChunksOnMap = true;
	public static boolean ownClaimedChunksOnMap = true;
	public static boolean inWorldWaypoints = true;
	public static boolean deathWaypoints = true;
	public static MapMode mapMode = MapMode.NONE;
	public static int waterHeightFactor = 8;

	public static MinimapPosition minimap = MinimapPosition.TOP_RIGHT;
	public static double minimapScale = 1D;
	public static boolean minimapLockedNorth = true;
	public static boolean minimapWaypoints = true;
	public static boolean minimapPlayerHeads = true;
	public static boolean minimapEntities = true;
	public static boolean minimapEntityHeads = true;
	public static boolean minimapLargeEntities = false;
	public static boolean minimapXYZ = true;
	public static boolean minimapBiome = true;
	public static boolean minimapBlur = true;
	public static boolean minimapCompass = true;
	public static int minimapVisibility = 255;
	public static boolean minimapZone = true;

	public static boolean debugInfo = false;
	public static int taskQueueTicks = 4;
	public static int rerenderQueueTicks = 60;
	public static int taskQueueMax = 100;

	/*
	private static Pair<ClientConfig, ForgeConfigSpec> client;

	public static void init() {
		FMLJavaModLoadingContext.get().getModEventBus().register(FTBChunksClientConfig.class);

		client = new ForgeConfigSpec.Builder().configure(ClientConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.CLIENT, client.getRight());
	}

	@SubscribeEvent
	public static void reload(ModConfig.ModConfigEvent event) {
		ModConfig config = event.getConfig();

		if (config.getSpec() == client.getRight()) {
			ClientConfig c = client.getLeft();
			noise = c.noise.get().floatValue();
			shadows = c.shadows.get().floatValue();
			chunkGrid = c.chunkGrid.get();
			reducedColorPalette = c.reducedColorPalette.get();
			saturation = c.saturation.get().floatValue();
			claimedChunksOnMap = c.claimedChunksOnMap.get();
			ownClaimedChunksOnMap = c.ownClaimedChunksOnMap.get();
			inWorldWaypoints = c.inWorldWaypoints.get();
			deathWaypoints = c.deathWaypoints.get();
			mapMode = c.mapMode.get();
			waterHeightFactor = c.waterHeightFactor.get();

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

			if (MapManager.inst != null) {
				MapManager.inst.updateAllRegions(false);
			}
		}
	}

	private static class ClientConfig {
		public final ForgeConfigSpec.DoubleValue noise;
		public final ForgeConfigSpec.DoubleValue shadows;
		public final ForgeConfigSpec.BooleanValue chunkGrid;
		public final ForgeConfigSpec.BooleanValue reducedColorPalette;
		public final ForgeConfigSpec.DoubleValue saturation;
		public final ForgeConfigSpec.BooleanValue claimedChunksOnMap;
		public final ForgeConfigSpec.BooleanValue ownClaimedChunksOnMap;
		public final ForgeConfigSpec.BooleanValue inWorldWaypoints;
		public final ForgeConfigSpec.BooleanValue deathWaypoints;
		public final ForgeConfigSpec.EnumValue<MapMode> mapMode;
		public final ForgeConfigSpec.IntValue waterHeightFactor;

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

		private ClientConfig(ForgeConfigSpec.Builder builder) {
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

			deathWaypoints = builder
					.comment("Enables creation of death waypoints")
					.translation("ftbchunks.death_waypoints")
					.define("death_waypoints", true);

			mapMode = builder
					.comment("Different ways to render map")
					.translation("ftbchunks.map_mode")
					.defineEnum("map_mode", MapMode.NONE);

			waterHeightFactor = builder
					.comment("How many blocks should height checks skip in water. 0 means flat water, ignoring terrain")
					.translation("ftbchunks.water_height_factor")
					.defineInRange("water_height_factor", 8, 0, 128);

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
	 */

	public static void init() {
	}

	public static void openSettings() {
		ConfigGroup group = new ConfigGroup("ftbchunks");

		group.addDouble("noise", noise, v -> noise = v.floatValue(), 0.05D, 0D, 0.5D);
		group.addDouble("shadows", shadows, v -> shadows = v.floatValue(), 0.1D, 0D, 0.3D);
		group.addBool("chunk_grid", chunkGrid, v -> chunkGrid = v, false);
		group.addBool("reduced_color_palette", reducedColorPalette, v -> reducedColorPalette = v, false);
		group.addDouble("saturation", saturation, v -> saturation = v.floatValue(), 1D, 0D, 1D);
		group.addBool("claimed_chunks_on_map", claimedChunksOnMap, v -> claimedChunksOnMap = v, true);
		group.addBool("own_claimed_chunks_on_map", ownClaimedChunksOnMap, v -> ownClaimedChunksOnMap = v, true);
		group.addBool("in_world_waypoints", inWorldWaypoints, v -> inWorldWaypoints = v, true);
		group.addBool("death_waypoints", deathWaypoints, v -> deathWaypoints = v, true);
		group.addEnum("map_mode", mapMode, v -> mapMode = v, NameMap.of(MapMode.NONE, MapMode.values()).create());
		group.addInt("water_height_factor", waterHeightFactor, v -> waterHeightFactor = v, 0, 0, 128);
		group.addEnum("minimap", minimap, v -> minimap = v, NameMap.of(MinimapPosition.TOP_RIGHT, MinimapPosition.values()).create());
		group.addDouble("minimap_scale", minimapScale, v -> minimapScale = v, 1D, 0.25D, 4D);
		group.addBool("minimap_locked_north", minimapLockedNorth, v -> minimapLockedNorth = v, true);
		group.addBool("minimap_waypoints", minimapWaypoints, v -> minimapWaypoints = v, true);
		group.addBool("minimap_entities", minimapEntities, v -> minimapEntities = v, true);
		group.addBool("minimap_entity_heads", minimapEntityHeads, v -> minimapEntityHeads = v, true);
		group.addBool("minimap_player_heads", minimapPlayerHeads, v -> minimapPlayerHeads = v, true);
		group.addBool("minimap_large_entities", minimapLargeEntities, v -> minimapLargeEntities = v, false);
		group.addBool("minimap_xyz", minimapXYZ, v -> minimapXYZ = v, true);
		group.addBool("minimap_biome", minimapBiome, v -> minimapBiome = v, true);
		group.addBool("minimap_blur", minimapBlur, v -> minimapBlur = v, true);
		group.addBool("minimap_compass", minimapCompass, v -> minimapCompass = v, true);
		group.addInt("minimap_visibility", minimapVisibility, v -> minimapVisibility = v, 255, 0, 255);
		group.addBool("minimap_zone", minimapZone, v -> minimapZone = v, true);
		group.addBool("debug_info", debugInfo, v -> debugInfo = v, false);

		EditConfigScreen gui = new EditConfigScreen(group);
		group.savedCallback = b -> {
			if (MapManager.inst != null) {
				MapManager.inst.updateAllRegions(false);
			}

			new LargeMapScreen().openGui();
		};

		gui.openGui();
	}
}
