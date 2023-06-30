package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftblibrary.config.NameMap;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public enum MinimapPosition {
	BOTTOM_LEFT(0, 2),
	LEFT(0, 1),
	TOP_LEFT(0, 0),
	TOP_RIGHT(1, 0),
	RIGHT(1, 1),
	BOTTOM_RIGHT(1, 2);

	public static final NameMap<MinimapPosition> NAME_MAP = NameMap.of(TOP_RIGHT, values()).baseNameKey("ftbchunks.minimap.position").create();

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

		return h - s - 5;
	}

	/**
	 * Used for applying a conditional check for the minimap offset. When set to none
	 * the offset will apply in all positions. When set, it will only apply to that position.
	 */
	public enum MinimapOffsetConditional implements Predicate<MinimapPosition> {
		BOTTOM_LEFT(MinimapPosition.BOTTOM_LEFT),
		LEFT(MinimapPosition.LEFT),
		TOP_LEFT(MinimapPosition.TOP_LEFT),
		TOP_RIGHT(MinimapPosition.TOP_RIGHT),
		RIGHT(MinimapPosition.RIGHT),
		BOTTOM_RIGHT(MinimapPosition.BOTTOM_RIGHT),
		NONE(null);

		public static final NameMap<MinimapOffsetConditional> NAME_MAP = NameMap.of(NONE, values()).baseNameKey("ftbchunks.minimap.position").create();

		@Nullable
		private final MinimapPosition position;

		MinimapOffsetConditional(@Nullable MinimapPosition position) {
			this.position = position;
		}

		@Override
		public boolean test(MinimapPosition minimapPosition) {
			return this == NONE || position == minimapPosition;
		}
	}
}
