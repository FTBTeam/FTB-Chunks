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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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

	@Comment("Shadow intensity")
	public float shadows = 0.1F;

	@Comment("Chunk grid overlay in large map")
	public boolean chunkGrid = false;

	@Comment("Reduces color palette to 256 colors")
	public boolean reducedColorPalette = false;

	@Comment("Color intensity")
	public float saturation = 1F;

	@Comment("Show claimed chunks on the map")
	public boolean claimedChunksOnMap = true;

	@Comment("Show your own claimed chunks on the map")
	public boolean ownClaimedChunksOnMap = true;

	@Comment("Show waypoints in world")
	public boolean inWorldWaypoints = true;

	@Comment("Enables creation of death waypoints")
	public boolean deathWaypoints = true;

	@Comment("Different ways to render map")
	public MapMode mapMode = MapMode.NONE;

	@Comment("How many blocks should height checks skip in water. 0 means flat water, ignoring terrain")
	public int waterHeightFactor = 8;

	@Comment("Enables minimap to show up in corner")
	public MinimapPosition minimap = MinimapPosition.TOP_RIGHT;

	@Comment("Scale of minimap")
	public double minimapScale = 1D;

	@Comment("Minimap will not rotate")
	public boolean minimapLockedNorth = true;

	@Comment("Show waypoints on minimap")
	public boolean minimapWaypoints = true;

	@Comment("Show player heads on minimap")
	public boolean minimapPlayerHeads = true;

	@Comment("Show entities on minimap")
	public boolean minimapEntities = true;

	@Comment("Show entity heads on minimap")
	public boolean minimapEntityHeads = true;

	@Comment("Entities in minimap will be larger")
	public boolean minimapLargeEntities = false;

	@Comment("Show XYZ under minimap")
	public boolean minimapXYZ = true;

	@Comment("Show biome under minimap")
	public boolean minimapBiome = true;

	@Comment("Blurs minimap")
	public boolean minimapBlur = true;

	@Comment("Adds NWSE compass inside minimap")
	public boolean minimapCompass = true;

	@Comment("Minimap visibility")
	public int minimapVisibility = 255;

	@Comment("Show zone (claimed chunk or wilderness) under minimap")
	public boolean minimapZone = true;

	@Comment("Enables debug info")
	public boolean debugInfo = false;

	@ConfigEntry.Gui.Excluded
	public int taskQueueTicks = 4;

	@ConfigEntry.Gui.Excluded
	public int rerenderQueueTicks = 60;

	@ConfigEntry.Gui.Excluded
	public int taskQueueMax = 100;

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

	public void openSettings(Screen screen) {
		//Minecraft.getInstance().setScreen(AutoConfig.getConfigScreen(FTBChunksClientConfig.class, screen).get());

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

			Minecraft.getInstance().setScreen(screen);
		};

		gui.openGui();
	}
}
