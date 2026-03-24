package dev.ftb.mods.ftbchunks.config;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.PointerIconMode;
import dev.ftb.mods.ftbchunks.client.gui.GuiClaimMode;
import dev.ftb.mods.ftbchunks.client.map.BiomeBlendMode;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapBlurMode;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapComponentConfig;
import dev.ftb.mods.ftbchunks.client.minimap.components.*;
import dev.ftb.mods.ftbchunks.util.EntityTypeBoolMapValue;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManager;
import dev.ftb.mods.ftblibrary.config.value.*;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.util.PanelPositioning;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.stream.Stream;

public interface FTBChunksClientConfig {
    String KEY = FTBChunks.MOD_ID + "-client";

    Config CONFIG = Config.create(KEY)
            .comment("Client-specific configuration for FTB Chunks",
                    "Modpack defaults should be defined in <instance>/config/" + KEY + ".snbt",
                    "  (may be overwritten on modpack update)",
                    "Players may locally override this by copying into <instance>/local/" + KEY + ".snbt",
                    "  (will NOT be overwritten on modpack update)"
            );

    Config APPEARANCE = CONFIG.addGroup("appearance", 0);
    DoubleValue NOISE = APPEARANCE.addDouble("noise", 0.05D, 0D, 0.5D)
            .comment("Noise added to map to make it look less plastic");
    DoubleValue SHADOWS = APPEARANCE.addDouble("shadows", 0.1D, 0D, 0.3D)
            .comment("Shadow intensity");
    BooleanValue CHUNK_GRID = APPEARANCE.addBoolean("chunk_grid", false)
            .comment("Chunk grid overlay in large map");
    BooleanValue REDUCED_COLOR_PALETTE = APPEARANCE.addBoolean("reduced_color_palette", false)
            .comment("Reduces color palette to 256 colors");
    DoubleValue SATURATION = APPEARANCE.addDouble("saturation", 1D, 0D, 1D
    ).comment("Color intensity");
    BooleanValue CLAIMED_CHUNKS_ON_MAP = APPEARANCE.addBoolean("claimed_chunks_on_map", true)
            .comment("Show claimed chunks on the map");
    BooleanValue OWN_CLAIMED_CHUNKS_ON_MAP = APPEARANCE.addBoolean("own_claimed_chunks_on_map", true)
            .comment("Show your own claimed chunks on the map");
    BooleanValue ONLY_SURFACE_ENTITIES = APPEARANCE.addBoolean("only_surface_entities", true)
            .comment("Only show entities that are on the surface");
    EnumValue<MapMode> MAP_MODE = APPEARANCE.addEnum("map_mode", MapMode.NAME_MAP)
            .comment("Different ways to render map");
    IntValue WATER_HEIGHT_FACTOR = APPEARANCE.addInt("water_height_factor", 8, 0, 128)
            .comment("How many blocks should height checks skip in water. 0 means flat water, ignoring terrain");
    EnumValue<BiomeBlendMode> BIOME_BLEND = APPEARANCE.addEnum("biome_blend", BiomeBlendMode.NAME_MAP)
            .comment("Biome blend");
    IntValue WATER_VISIBILITY = APPEARANCE.addInt("water_visibility", 220, 0, 255)
            .excludedFromGui().comment("Advanced option. Water visibility");
    IntValue GRASS_DARKNESS = APPEARANCE.addInt("grass_darkness", 50, 0, 255)
            .excludedFromGui().comment("Advanced option. Grass darkness");
    IntValue FOLIAGE_DARKNESS = APPEARANCE.addInt("foliage_darkness", 50, 0, 255)
            .excludedFromGui().comment("Advanced option. Foliage darkness");
    EnumValue<PointerIconMode> POINTER_ICON_MODE = APPEARANCE.addEnum("pointer_icon_mode", PointerIconMode.NAME_MAP)
            .comment("Mode for the pointer icon to render on full screen map");
    EnumValue<GuiClaimMode> CLAIM_MODE = APPEARANCE.addEnum("claim_mode", GuiClaimMode.NAME_MAP)
            .comment("Claim mode for the chunk claiming screen");

    Config WAYPOINTS = CONFIG.addGroup("waypoints", 1);
    BooleanValue IN_WORLD_WAYPOINTS = WAYPOINTS.addBoolean("in_world_waypoints", true)
            .comment("Show waypoints in world");
    BooleanValue DEATH_WAYPOINTS = WAYPOINTS.addBoolean("death_waypoints", true)
            .comment("Enables creation of death waypoints");
    IntValue DEATH_WAYPOINT_AUTOREMOVE_DISTANCE = WAYPOINTS.addInt("death_waypoint_autoremove_distance", 0, 0, Integer.MAX_VALUE)
            .comment("Automatically remove death waypoints if closer than this many blocks away (distance of 0 disables removal)");
    DoubleValue WAYPOINT_BEACON_FADE_DISTANCE = WAYPOINTS.addDouble("waypoint_fade_distance", 12D, 1D, 200D)
            .comment("Minimum distance before waypoint beacons start to fade");
    DoubleValue WAYPOINT_DOT_FADE_DISTANCE = WAYPOINTS.addDouble("waypoint_dot_fade_distance", 1D, 1D, 200D)
            .comment("Minimum distance before waypoint dots start to fade");
    DoubleValue WAYPOINT_MAX_DISTANCE = WAYPOINTS.addDouble("waypoint_max_distance", 5000D, 1D, Integer.MAX_VALUE)
            .comment("Maximum distance at which waypoints are drawn");
    DoubleValue WAYPOINT_FOCUS_DISTANCE = WAYPOINTS.addDouble("waypoint_focus_distance", 1d, 1d, 10d)
            .comment("How close player crosshair needs to be to in-world waypoints to show waypoint labels");
    DoubleValue WAYPOINT_FOCUS_SCALE = WAYPOINTS.addDouble("waypoint_focus_scale", 2d, 1d, 10d)
            .comment("How much do in-world waypoints enlarge when the player crosshair is close");

    Config MINIMAP = CONFIG.addGroup("minimap", 2);
    BooleanValue MINIMAP_ENABLED = MINIMAP.addBoolean("enabled", !hasOtherMinimapMod())
            .comment("Enable minimap");
    EnumValue<PanelPositioning> MINIMAP_POSITION = MINIMAP.addEnum("position", PanelPositioning.NAME_MAP)
            .comment("Minimap screen positioning");
    DoubleValue MINIMAP_SCALE = MINIMAP.addDouble("scale", 1D, 0.25D, 4D)
            .comment("Scale of minimap");
    DoubleValue MINIMAP_ZOOM = MINIMAP.addDouble("zoom", 1D, 1D, 4D)
            .comment("Zoom distance of the minimap");
    BooleanValue MINIMAP_LOCKED_NORTH = MINIMAP.addBoolean("locked_north", true)
            .comment("When true, minimap rotation is locked to North = Up");
    EnumValue<MinimapBlurMode> MINIMAP_BLUR_MODE = MINIMAP.addEnum("blur_mode", MinimapBlurMode.NAME_MAP)
            .comment("Blurs minimap");
    BooleanValue MINIMAP_COMPASS = MINIMAP.addBoolean("compass", true)
            .comment("Add compass points at N/E/S/W on the minimap edge");
    BooleanValue MINIMAP_RETICLE = MINIMAP.addBoolean("reticle", true)
            .comment("Draw crosshair lines on minimap");
    IntValue MINIMAP_ALPHA = MINIMAP.addInt("visibility", 255, 0, 255)
            .comment("Minimap alpha level: 255 = opaque, 0 = fully transparent (invisible)");
    IntValue MINIMAP_OFFSET_X = MINIMAP.addInt("position_offset_x", 5)
            .comment("X positioning offset, always toward center of screen");
    IntValue MINIMAP_OFFSET_Y = MINIMAP.addInt("position_offset_y", 5)
            .comment("Y positioning offset, always toward center of screen");
    BooleanValue SQUARE_MINIMAP = MINIMAP.addBoolean("square", false)
        .comment("Draw a square minimap instead of a circular one (also enforces rotation locking)");
    BooleanValue MINIMAP_PROPORTIONAL = MINIMAP.addBoolean("proportional", true)
            .comment("Size minimap proportional to screen width (and scale)");

    Config MINIMAP_ICONS = MINIMAP.addGroup("icons");
    EntityTypeBoolMapValue ENTITY_ICON = MINIMAP_ICONS.add(new EntityTypeBoolMapValue(MINIMAP, "entity_icon", Collections.emptyMap()))
            .comment("Entity icons on minimap");
    EnumValue<PointerIconMode> POINTER_ICON_MODE_MINIMAP = MINIMAP_ICONS.addEnum("pointer_icon_mode_minimap", PointerIconMode.NAME_MAP)
            .comment("Mode for the pointer icon to render on minimap");
    BooleanValue MINIMAP_ENTITIES = MINIMAP_ICONS.addBoolean("entities", true)
            .comment("Show entity icons on the minimap");
    BooleanValue MINIMAP_LARGE_ENTITIES = MINIMAP_ICONS.addBoolean("large_entities", false)
            .comment("Make entity icons on the minimap larger");
    BooleanValue SHOW_PLAYER_WHEN_UNLOCKED = MINIMAP_ICONS.addBoolean("show_player_when_unlocked", true)
            .comment("Always show player icon on minimap, even when rotation not locked");
    BooleanValue MINIMAP_WAYPOINTS = MINIMAP_ICONS.addBoolean("waypoints", true)
            .comment("Show waypoint icons on minimap");
    BooleanValue MINIMAP_PLAYER_HEADS = MINIMAP_ICONS.addBoolean("player_heads", true)
            .comment("Show other player heads on minimap");

    Config MINIMAP_INFO = MINIMAP.addGroup("info");
    DoubleValue MINIMAP_FONT_SCALE = MINIMAP_INFO.addDouble("font_scale", 0.5, 0.1, 5.0)
            .comment("Minimap font scaling (values not a multiple of 0.25 may look bad)");
    StringMapValue MINIMAP_SETTINGS = MINIMAP_INFO.add(new MinimapComponentConfig(MINIMAP, "info_settings", Collections.emptyMap()))
            .comment("Settings for minimap info components");
    BooleanValue TEXT_ABOVE_MINIMAP = MINIMAP_INFO.addBoolean("text_above_minimap", false)
            .comment("Show text above minimap");
    StringListValue MINIMAP_INFO_ORDER = MINIMAP_INFO.addStringList("info_order", Stream.of(PlayerPosInfoComponent.ID, BiomeComponent.ID, ZoneInfoComponent.ID, FPSComponent.ID, GameTimeComponent.ID, RealTimeComponent.ID, DebugComponent.ID).map(Identifier::toString).toList())
            .excludedFromGui()
            .comment("Info displayed under minimap");
    StringListValue MINIMAP_INFO_HIDDEN = MINIMAP_INFO.addStringList("info_hidden", Stream.of(FPSComponent.ID, GameTimeComponent.ID, RealTimeComponent.ID, DebugComponent.ID).map(Identifier::toString).toList())
            .excludedFromGui()
            .comment("Info hidden under minimap");

    Config ADVANCED = CONFIG.addGroup("advanced", 3);
    BooleanValue DEBUG_INFO = ADVANCED.addBoolean("debug_info", false).comment("Enables debug info");
    IntValue TASK_QUEUE_TICKS = ADVANCED.addInt("task_queue_ticks", 4, 1, 300).excludedFromGui().comment("Advanced option. How often queued tasks will run");
    IntValue RERENDER_QUEUE_TICKS = ADVANCED.addInt("rerender_queue_ticks", 60, 1, 600).excludedFromGui().comment("Advanced option. How often map render update will be queued");
    IntValue TASK_QUEUE_MAX = ADVANCED.addInt("task_queue_max", 100, 1, 10000).excludedFromGui().comment("Advanced option. Max tasks that can queue up");
    IntValue MINIMAP_ICON_UPDATE_TIMER = ADVANCED.addInt("minimap_icon_update_timer", 500, 0, 10000).excludedFromGui().comment("Advanced option. Change how often the minimap will refresh icons");

    Config MEMORY = ADVANCED.addGroup("memory", 4);
    IntValue REGION_RELEASE_TIME = MEMORY.addInt("region_release_time", 300, 0, Integer.MAX_VALUE).comment("Periodically release region data for non-recently-used regions to save memory (units of seconds, 0 disables releasing");
    IntValue AUTORELEASE_ON_MAP_CLOSE = MEMORY.addInt("autorelease_on_map_close", 32, 0, Integer.MAX_VALUE).comment("When the large map is closed, auto-release least recently accessed regions down to this number (0 disables releasing)");
    BooleanValue MAX_ZOOM_CONSTRAINT = MEMORY.addBoolean("max_zoom_constraint", true).comment("Constrain maximum map zoom-out based on number of explored regions and available memory");

    static boolean hasOtherMinimapMod() {
        return Platform.get().isModLoaded("journeymap") || Platform.get().isModLoaded("voxelmap") || Platform.get().isModLoaded("antiqueatlas") || Platform.get().isModLoaded("xaerominimap");
    }

    static void saveConfig() {
        ConfigManager.getInstance().save(KEY);
    }

    static boolean shouldBlurTexture(double zoom) {
        MinimapBlurMode blurMode = MINIMAP_BLUR_MODE.get();
        return blurMode == MinimapBlurMode.AUTO ? zoom < 1.5 : blurMode == MinimapBlurMode.ON;
    }
}
