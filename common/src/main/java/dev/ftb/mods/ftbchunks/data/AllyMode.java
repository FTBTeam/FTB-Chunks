package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum AllyMode {
	DEFAULT,
	FORCED_ALL,
	FORCED_NONE;

	public static final NameMap<AllyMode> NAME_MAP = NameMap.of(DEFAULT, values()).baseNameKey("ftbchunks.ally_mode").create();
}