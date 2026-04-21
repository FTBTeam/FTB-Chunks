package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;

public class BlockColors {
	private static final HashMap<String, BlockColor> TYPE_MAP = new HashMap<>();

	public static final BlockColor ERROR = register("error",
			(_, _) -> Color4I.RED);
	public static final BlockColor FOLIAGE = register("foliage",
			(level, pos) -> Color4I.rgb(BiomeColors.getAverageFoliageColor(level, pos)));
	public static final BlockColor GRASS = register("grass",
			(level, pos) -> Color4I.rgb(BiomeColors.getAverageGrassColor(level, pos)));
	public static final BlockColor DYNAMIC = register("dynamic", DynamicBlockColor.INSTANCE);
	public static final BlockColor IGNORED = register("ignored", IgnoredBlockColor.INSTANCE);
	public static final BlockColor BOP_RAINBOW = register("bop_rainbow",
			(_, pos) -> Color4I.hsb((((float) pos.getX() + Mth.sin(((float) pos.getZ() + (float) pos.getX()) / 35) * 35) % 150) / 150, 0.6F, 0.5F));

	public static BlockColor register(String type, BlockColor color) {
		TYPE_MAP.put(type, color);
		return color;
	}

	@Nullable
	public static BlockColor getFromType(String value) {
		if (value.indexOf('#') == 0) {
			return new CustomBlockColor(Color4I.parse(value));
		} else {
			return TYPE_MAP.get(value);
		}
	}
}
