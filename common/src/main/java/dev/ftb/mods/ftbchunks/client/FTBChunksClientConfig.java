package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.BiomeBlendMode;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.snbt.config.BooleanValue;
import dev.ftb.mods.ftblibrary.snbt.config.DoubleValue;
import dev.ftb.mods.ftblibrary.snbt.config.EnumValue;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import me.shedaniel.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * @author LatvianModder
 */
public interface FTBChunksClientConfig {
	SNBTConfig CONFIG = SNBTConfig.create(FTBChunks.MOD_ID + "-client");

	DoubleValue NOISE = CONFIG.getDouble("noise", 0.05D, 0D, 0.5D).fader().comment("Noise added to map to make it look less plastic");
	DoubleValue SHADOWS = CONFIG.getDouble("shadows", 0.1D, 0D, 0.3D).fader().comment("Shadow intensity");
	BooleanValue CHUNK_GRID = CONFIG.getBoolean("chunk_grid", false).comment("Chunk grid overlay in large map");
	BooleanValue REDUCED_COLOR_PALETTE = CONFIG.getBoolean("reduced_color_palette", false).comment("Reduces color palette to 256 colors");
	DoubleValue SATURATION = CONFIG.getDouble("saturation", 1D, 0D, 1D).fader().comment("Color intensity");
	BooleanValue CLAIMED_CHUNKS_ON_MAP = CONFIG.getBoolean("claimed_chunks_on_map", true).comment("Show claimed chunks on the map");
	BooleanValue OWN_CLAIMED_CHUNKS_ON_MAP = CONFIG.getBoolean("own_claimed_chunks_on_map", true).comment("Show your own claimed chunks on the map");
	BooleanValue IN_WORLD_WAYPOINTS = CONFIG.getBoolean("in_world_waypoints", true).comment("Show waypoints in world");
	BooleanValue DEATH_WAYPOINTS = CONFIG.getBoolean("death_waypoints", true).comment("Enables creation of death waypoints");
	EnumValue<MapMode> MAP_MODE = CONFIG.getEnum("map_mode", MapMode.NAME_MAP).comment("Different ways to render map");
	IntValue WATER_HEIGHT_FACTOR = CONFIG.getInt("water_height_factor", 8, 0, 128).comment("How many blocks should height checks skip in water. 0 means flat water, ignoring terrain");
	EnumValue<BiomeBlendMode> BIOME_BLEND = CONFIG.getEnum("biome_blend", BiomeBlendMode.NAME_MAP).comment("Biome blend");

	SNBTConfig MINIMAP = CONFIG.getGroup("minimap");

	BooleanValue MINIMAP_ENABLED = MINIMAP.getBoolean("enabled", !hasOtherMinimapMod()).comment("Enable minimap");
	EnumValue<MinimapPosition> MINIMAP_POSITION = MINIMAP.getEnum("position", MinimapPosition.NAME_MAP).comment("Enables minimap to show up in corner");
	DoubleValue MINIMAP_SCALE = MINIMAP.getDouble("scale", 1D, 0.25D, 4D).comment("Scale of minimap");
	BooleanValue MINIMAP_LOCKED_NORTH = MINIMAP.getBoolean("locked_north", true).comment("Minimap will not rotate");
	BooleanValue MINIMAP_WAYPOINTS = MINIMAP.getBoolean("waypoints", true).comment("Show waypoints on minimap");
	BooleanValue MINIMAP_PLAYER_HEADS = MINIMAP.getBoolean("player_heads", true).comment("Show player heads on minimap");
	BooleanValue MINIMAP_ENTITIES = MINIMAP.getBoolean("entities", true).comment("Show entities on minimap");
	BooleanValue MINIMAP_ENTITY_HEADS = MINIMAP.getBoolean("entity_heads", true).comment("Show entity heads on minimap");
	BooleanValue MINIMAP_LARGE_ENTITIES = MINIMAP.getBoolean("large_entities", false).comment("Entities in minimap will be larger");
	BooleanValue MINIMAP_XYZ = MINIMAP.getBoolean("xyz", true).comment("Show XYZ under minimap");
	BooleanValue MINIMAP_BIOME = MINIMAP.getBoolean("biome", true).comment("Show biome under minimap");
	BooleanValue MINIMAP_BLUR = MINIMAP.getBoolean("blur", false).comment("Blurs minimap");
	BooleanValue MINIMAP_COMPASS = MINIMAP.getBoolean("compass", true).comment("Adds NWSE compass inside minimap");
	IntValue MINIMAP_VISIBILITY = MINIMAP.getInt("visibility", 255, 0, 255).comment("Minimap visibility");
	BooleanValue MINIMAP_ZONE = MINIMAP.getBoolean("zone", true).comment("Show zone (claimed chunk or wilderness) under minimap");
	DoubleValue MINIMAP_ZOOM = MINIMAP.getDouble("zoom", 1D, 1D, 4D).comment("Zoom distance of the minimap");

	BooleanValue DEBUG_INFO = CONFIG.getBoolean("debug_info", false).comment("Enables debug info");
	IntValue TASK_QUEUE_TICKS = CONFIG.getInt("task_queue_ticks", 4, 1, 300).excluded().comment("Advanced option. How often queued tasks will run");
	IntValue RERENDER_QUEUE_TICKS = CONFIG.getInt("rerender_queue_ticks", 60, 1, 600).excluded().comment("Advanced option. How often map render update will be queued");
	IntValue TASK_QUEUE_MAX = CONFIG.getInt("task_queue_max", 100, 1, 10000).excluded().comment("Advanced option. Max tasks that can queue up");
	IntValue WATER_VISIBILITY = CONFIG.getInt("water_visibility", 220, 0, 255).excluded().comment("Advanced option. Water visibility");
	IntValue GRASS_DARKNESS = CONFIG.getInt("grass_darkness", 50, 0, 255).excluded().comment("Advanced option. Grass darkness");
	IntValue FOLIAGE_DARKNESS = CONFIG.getInt("foliage_darkness", 50, 0, 255).excluded().comment("Advanced option. Foliage darkness");

	static boolean hasOtherMinimapMod() {
		return Platform.isModLoaded("journeymap") || Platform.isModLoaded("voxelmap");
	}

	static void init() {
		CONFIG.load(Platform.getGameFolder().resolve("local/ftbchunks/client-config.snbt"));
	}

	static void openSettings(Screen screen) {
		ConfigGroup group = new ConfigGroup("ftbchunks");
		CONFIG.createClientConfig(group);

		EditConfigScreen gui = new EditConfigScreen(group);
		group.savedCallback = b -> {
			if (b) {
				saveConfig();
			}

			if (MapManager.inst != null) {
				MapManager.inst.updateAllRegions(false);
			}

			Minecraft.getInstance().setScreen(screen);
		};

		gui.openGui();
	}

	static void saveConfig() {
		CONFIG.save(Platform.getGameFolder().resolve("local/ftbchunks/client-config.snbt"));
	}
}
