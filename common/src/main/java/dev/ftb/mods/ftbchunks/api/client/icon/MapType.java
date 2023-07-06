package dev.ftb.mods.ftbchunks.api.client.icon;

public enum MapType {
	/**
	 * An icon drawn on the large (full-screen) map
	 */
	LARGE_MAP,
	/**
	 * An icon drawn on the minimap
	 */
	MINIMAP,
	/**
	 * An icon drawn in 3d game view (just waypoints by default)
	 */
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
