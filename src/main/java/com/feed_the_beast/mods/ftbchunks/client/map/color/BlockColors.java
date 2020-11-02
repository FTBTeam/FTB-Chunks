package com.feed_the_beast.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.BiomeColors;

import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class BlockColors
{
	private static final HashMap<String, BlockColor> TYPE_MAP = new HashMap<>();
	private static final Color4I[] REDSTONE_0_15_COLORS = new Color4I[16];

	static
	{
		for (int power = 0; power < 16; power++)
		{
			float f = power / 15F;
			float r0 = f * 0.6F + 0.4F;
			if (power == 0)
			{
				r0 = 0.3F;
			}

			float g0 = f * f * 0.7F - 0.5F;
			float b0 = f * f * 0.6F - 0.7F;

			if (g0 < 0F)
			{
				g0 = 0F;
			}

			if (b0 < 0F)
			{
				b0 = 0F;
			}

			int r = MathHelper.clamp((int) (r0 * 255F), 0, 255);
			int g = MathHelper.clamp((int) (g0 * 255F), 0, 255);
			int b = MathHelper.clamp((int) (b0 * 255F), 0, 255);
			REDSTONE_0_15_COLORS[power] = Color4I.rgb(r, g, b);
		}
	}

	public static BlockColor register(String type, BlockColor color)
	{
		TYPE_MAP.put(type, color);
		return color;
	}

	public static final BlockColor DEFAULT = register("default", (state, world, pos) -> {
		MaterialColor materialColor = state.getMaterialColor(world, pos);
		return materialColor == null ? Color4I.RED : Color4I.rgb(materialColor.colorValue);
	});

	public static final BlockColor FOLIAGE = register("foliage", (state, world, pos) -> Color4I.rgb(BiomeColors.getFoliageColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50)));
	public static final BlockColor GRASS = register("grass", (state, world, pos) -> Color4I.rgb(BiomeColors.getGrassColor(world, pos)).withTint(Color4I.BLACK.withAlpha(50)));
	public static final BlockColor IGNORED = register("ignored", new IgnoredBlockColor());

	public static final BlockColor REDSTONE_0_15 = register("redstone_0_15", (state, world, pos) -> REDSTONE_0_15_COLORS[state.get(BlockStateProperties.POWER_0_15)]);
	public static final BlockColor BOP_RAINBOW = register("bop_rainbow", (state, world, pos) -> Color4I.hsb((((float) pos.getX() + MathHelper.sin(((float) pos.getZ() + (float) pos.getX()) / 35) * 35) % 150) / 150, 0.6F, 0.5F));

	public static BlockColor getFromType(String value)
	{
		if (value.indexOf('#') == 0)
		{
			return new CustomBlockColor(Color4I.fromString(value));
		}
		else
		{
			return TYPE_MAP.getOrDefault(value, DEFAULT);
		}
	}
}
