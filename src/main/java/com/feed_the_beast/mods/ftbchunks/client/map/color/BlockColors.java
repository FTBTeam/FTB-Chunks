package com.feed_the_beast.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class BlockColors {
	private static final HashMap<String, BlockColor> TYPE_MAP = new HashMap<>();

	public static BlockColor register(String type, BlockColor color) {
		TYPE_MAP.put(type, color);
		return color;
	}

	public static final BlockColor FOLIAGE = register("foliage", (world, pos) -> Color4I.rgb(BiomeColors.getAverageFoliageColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50)));
	public static final BlockColor GRASS = register("grass", (world, pos) -> Color4I.rgb(BiomeColors.getAverageGrassColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50)));
	public static final BlockColor IGNORED = register("ignored", new IgnoredBlockColor());

	public static final BlockColor BOP_RAINBOW = register("bop_rainbow", (world, pos) -> Color4I.hsb((((float) pos.getX() + Mth.sin(((float) pos.getZ() + (float) pos.getX()) / 35) * 35) % 150) / 150, 0.6F, 0.5F));

	@Nullable
	public static BlockColor getFromType(String value) {
		if (value.indexOf('#') == 0) {
			return new CustomBlockColor(Color4I.fromString(value));
		} else {
			return TYPE_MAP.get(value);
		}
	}
}
