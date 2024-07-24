package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum ForceLoadMode {
	DEFAULT,
	ALWAYS,
	NEVER;

	public static final NameMap<ForceLoadMode> NAME_MAP = NameMap.of(DEFAULT, values()).baseNameKey("ftbchunks.force_load_mode").create();
}
