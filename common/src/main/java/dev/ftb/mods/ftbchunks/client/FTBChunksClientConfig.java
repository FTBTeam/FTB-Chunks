package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftbguilibrary.config.ConfigGroup;
import dev.ftb.mods.ftbguilibrary.config.NameMap;
import dev.ftb.mods.ftbguilibrary.config.gui.EditConfigScreen;
import me.shedaniel.architectury.platform.Platform;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import net.minecraft.world.InteractionResult;

import java.io.File;

/**
 * @author LatvianModder
 */
@Config(name = "ftbchunks-client")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class FTBChunksClientConfig implements ConfigData {
	@ConfigEntry.Gui.Excluded
	private static ConfigHolder<FTBChunksClientConfig> holder = null;

	public static FTBChunksClientConfig get() {
		return holder.get();
	}

	@Comment("Noise added to map to make it look less plastic")
	public float noise = 0.05F;
	public float shadows = 0.1F;
	public boolean chunkGrid = false;
	public boolean reducedColorPalette = false;
	public float saturation = 1F;
	public boolean claimedChunksOnMap = true;
	public boolean ownClaimedChunksOnMap = true;
	public boolean inWorldWaypoints = true;
	public boolean deathWaypoints = true;
	public MapMode mapMode = MapMode.NONE;
	public int waterHeightFactor = 8;

	public MinimapPosition minimap = MinimapPosition.TOP_RIGHT;
	public double minimapScale = 1D;
	public boolean minimapLockedNorth = true;
	public boolean minimapWaypoints = true;
	public boolean minimapPlayerHeads = true;
	public boolean minimapEntities = true;
	public boolean minimapEntityHeads = true;
	public boolean minimapLargeEntities = false;
	public boolean minimapXYZ = true;
	public boolean minimapBiome = true;
	public boolean minimapBlur = true;
	public boolean minimapCompass = true;
	public int minimapVisibility = 255;
	public boolean minimapZone = true;

	@ConfigEntry.Gui.Excluded
	public boolean debugInfo = false;

	@ConfigEntry.Gui.Excluded
	public int taskQueueTicks = 4;

	@ConfigEntry.Gui.Excluded
	public int rerenderQueueTicks = 60;

	@ConfigEntry.Gui.Excluded
	public int taskQueueMax = 100;

	/*
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
		holder = AutoConfig.register(FTBChunksClientConfig.class, JanksonConfigSerializer::new);

		holder.registerLoadListener((manager, data) -> {
			File oldConfig = Platform.getConfigFolder().resolve("ftbchunks-client.toml").toFile();
			if (oldConfig.exists()) {
				FTBChunks.LOGGER.warn("Old config file ftbchunks-client.toml found, please use the new config format instead!");
				FTBChunks.LOGGER.warn("The old config file will automatically be deleted on exit.");
				oldConfig.deleteOnExit();
			}
			return InteractionResult.PASS;
		});

		holder.registerSaveListener((manager, data) -> {
			data.validatePostLoad();
			return InteractionResult.PASS;
		});
	}

	@Override
	public void validatePostLoad() {
		// maxBlocks = Mth.clamp(maxBlocks, 1, 32768);
	}

	public void openSettings() {
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
			if (b) {
				holder.save();
			}

			if (MapManager.inst != null) {
				MapManager.inst.updateAllRegions(false);
			}

			new LargeMapScreen().openGui();
		};

		gui.openGui();
	}
}
