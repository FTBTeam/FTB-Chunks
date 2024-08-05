package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum MapMode {
	NONE,
	NIGHT,
	TOPOGRAPHY,
	BLOCKS,
	LIGHT_SOURCES;

	public static final NameMap<MapMode> NAME_MAP = NameMap.of(NONE, values()).baseNameKey("ftbchunks.map_mode").create();
}
