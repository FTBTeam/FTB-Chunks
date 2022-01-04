package dev.ftb.mods.ftbchunks.client;

public enum MapType {
	LARGE_MAP(false),
	MINIMAP(true);

	private final boolean minimap;

	MapType(boolean m) {
		minimap = m;
	}

	public boolean isMinimap() {
		return minimap;
	}
}
