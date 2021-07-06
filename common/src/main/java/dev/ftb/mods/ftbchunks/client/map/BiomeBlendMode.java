package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.config.NameMap;
import net.minecraft.util.StringRepresentable;

public enum BiomeBlendMode implements StringRepresentable {
	NONE("none", 0),
	BLEND_3X3("blend_3x3", 1),
	BLEND_5X5("blend_5x5", 2),
	BLEND_7X7("blend_7x7", 3),
	BLEND_9X9("blend_9x9", 4),
	BLEND_11X11("blend_11x11", 5),
	BLEND_13X13("blend_13x13", 6),
	BLEND_15X15("blend_15x15", 7);

	public static final NameMap<BiomeBlendMode> NAME_MAP = NameMap.of(BLEND_5X5, values()).baseNameKey("ftbchunks.biome_blend").create();

	public final String name;
	public final int blend;

	BiomeBlendMode(String n, int b) {
		name = n;
		blend = b;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
