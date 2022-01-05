package dev.ftb.mods.ftbchunks.client;

public enum MapType {
	LARGE_MAP,
	MINIMAP,
	WORLD_ICON;

	public boolean isLargeMap() {
		return this == LARGE_MAP;
	}

	public boolean isMinimap() {
		return this == MINIMAP;
	}

	public boolean isWorldIcon() {
		return this == WORLD_ICON;
	}
}
