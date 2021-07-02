package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftblibrary.config.NameMap;

/**
 * @author LatvianModder
 */
public enum MinimapPosition {
	DISABLED(-1, -1),
	BOTTOM_LEFT(0, 2),
	LEFT(0, 1),
	TOP_LEFT(0, 0),
	TOP_RIGHT(1, 0),
	RIGHT(1, 1),
	BOTTOM_RIGHT(1, 2);

	public static final NameMap<MinimapPosition> NAME_MAP = NameMap.of(TOP_RIGHT, values()).create();

	public final int posX;
	public final int posY;

	MinimapPosition(int x, int y) {
		posX = x;
		posY = y;
	}

	public int getX(int w, int s) {
		if (posX == 0) {
			return 5;
		}

		return w - s - 5;
	}

	public int getY(int h, int s) {
		if (posY == 0) {
			return 5;
		} else if (posY == 1) {
			return (h - s) / 2;
		}

		return h - s - 20;
	}
}