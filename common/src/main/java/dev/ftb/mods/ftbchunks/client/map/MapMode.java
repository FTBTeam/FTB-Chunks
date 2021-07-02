package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.config.NameMap;

/**
 * @author LatvianModder
 */
public enum MapMode {
	NONE,
	NIGHT,
	TOPOGRAPHY,
	BLOCKS,
	BIOME_TEMPERATURE,
	LIGHT_SOURCES;

	public static final NameMap<MapMode> NAME_MAP = NameMap.of(NONE, values()).create();
}
