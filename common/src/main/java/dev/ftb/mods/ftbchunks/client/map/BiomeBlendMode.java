package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;

public enum BiomeBlendMode implements StringRepresentable {
	NONE("none", 0,
			"O"
	),

	BLEND_3X3("blend_3x3", 1,
			"OOO",
			"OOO",
			"OOO"
	),

	BLEND_5X5("blend_5x5", 2,
			" OOO ",
			"OOOOO",
			"OOOOO",
			"OOOOO",
			" OOO "
	),

	BLEND_7X7("blend_7x7", 3,
			"   O   ",
			" OOOOO ",
			" OOOOO ",
			"OOOOOOO",
			" OOOOO ",
			" OOOOO ",
			"   O   "
	),

	BLEND_9X9("blend_9x9", 4,
			"   OOO   ",
			" OOOOOOO ",
			" OOOOOOO ",
			"OOOOOOOOO",
			"OOOOOOOOO",
			"OOOOOOOOO",
			" OOOOOOO ",
			" OOOOOOO ",
			"   OOO   "
	),

	BLEND_11X11("blend_11x11", 5,
			"   OOOOO   ",
			" OOOOOOOOO ",
			" OOOOOOOOO ",
			"OOOOOOOOOOO",
			"OOOOOOOOOOO",
			"OOOOOOOOOOO",
			"OOOOOOOOOOO",
			"OOOOOOOOOOO",
			" OOOOOOOOO ",
			" OOOOOOOOO ",
			"   OOOOO   "
	),

	BLEND_13X13("blend_13x13", 6,
			"    OOOOO    ",
			"  OOOOOOOOO  ",
			" OOOOOOOOOOO ",
			" OOOOOOOOOOO ",
			"OOOOOOOOOOOOO",
			"OOOOOOOOOOOOO",
			"OOOOOOOOOOOOO",
			"OOOOOOOOOOOOO",
			"OOOOOOOOOOOOO",
			" OOOOOOOOOOO ",
			" OOOOOOOOOOO ",
			"  OOOOOOOOO  ",
			"    OOOOO    "
	),

	BLEND_15X15("blend_15x15", 7,
			"    OOOOOOO    ",
			"  OOOOOOOOOOO  ",
			" OOOOOOOOOOOOO ",
			" OOOOOOOOOOOOO ",
			"OOOOOOOOOOOOOOO",
			"OOOOOOOOOOOOOOO",
			"OOOOOOOOOOOOOOO",
			"OOOOOOOOOOOOOOO",
			"OOOOOOOOOOOOOOO",
			"OOOOOOOOOOOOOOO",
			"OOOOOOOOOOOOOOO",
			" OOOOOOOOOOOOO ",
			" OOOOOOOOOOOOO ",
			"  OOOOOOOOOOO  ",
			"    OOOOOOO    "
	);

	public static final NameMap<BiomeBlendMode> NAME_MAP = NameMap.of(BLEND_5X5, values()).baseNameKey("ftbchunks.biome_blend").create();

	private final String name;
	private final int blendRadius;
	private final int[] posX;
	private final int[] posY;
	private final int size;

	BiomeBlendMode(String name, int blendRadius, String... roundPattern) {
		this.name = name;
		this.blendRadius = blendRadius;

		size = (this.blendRadius * 2 + 1) * (this.blendRadius * 2 + 1);
		int[] posX0 = new int[size];
		int[] posY0 = new int[size];

		int n = 0;
		for (int y = 0; y < this.blendRadius * 2 + 1; y++) {
			for (int x = 0; x < this.blendRadius * 2 + 1; x++) {
				if (roundPattern[y].charAt(x) != ' ') {
					posX0[n] = x - this.blendRadius;
					posY0[n] = y - this.blendRadius;
					n++;
				}
			}
		}

		posX = Arrays.copyOf(posX0, size);
		posY = Arrays.copyOf(posY0, size);
	}

	public int getBlendRadius() {
		return blendRadius;
	}

	public Color4I doBlending(int[][] colors, int ax, int az) {
		if (blendRadius == 0) {
			return Color4I.rgb(colors[ax][az]);
		}

		int r = 0;
		int g = 0;
		int b = 0;
		int nColors = 0;

		for (int i = 0; i < size; i++) {
			int col = colors[ax + blendRadius + posX[i]][az + blendRadius + posY[i]];
			if (col != 0) {
				r += (col >> 16) & 0xFF;
				g += (col >> 8) & 0xFF;
				b += col & 0xFF;
				nColors++;
			}
		}

		return nColors == 0 ?
				Color4I.rgb(colors[ax + blendRadius][az + blendRadius]) :
				Color4I.rgb(r / nColors, g / nColors, b / nColors);

	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
