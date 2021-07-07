package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.config.NameMap;
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

	public final String name;
	public final int blend;

	public final int[] posX;
	public final int[] posY;
	public int size;

	BiomeBlendMode(String n, int b, String... roundPattern) {
		name = n;
		blend = b;

		int[] posX0 = new int[(blend * 2 + 1) * (blend * 2 + 1)];
		int[] posY0 = new int[posX0.length];
		size = 0;

		for (int y = 0; y < blend * 2 + 1; y++) {
			for (int x = 0; x < blend * 2 + 1; x++) {
				if (roundPattern[y].charAt(x) != ' ') {
					posX0[size] = x - blend;
					posY0[size] = y - blend;
					size++;
				}
			}
		}

		posX = Arrays.copyOf(posX0, size);
		posY = Arrays.copyOf(posY0, size);
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
