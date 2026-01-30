package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftblibrary.util.NameMap;

public enum MinimapBlurMode {
	AUTO,
	ON,
	OFF;

	public static final NameMap<MinimapBlurMode> NAME_MAP = NameMap.of(AUTO, values()).baseNameKey("ftbchunks.minimap.blur_mode").create();
}
