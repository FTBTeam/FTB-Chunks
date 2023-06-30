package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

public class CustomBlockColor implements BlockColor {
	private final Color4I color;

	public CustomBlockColor(Color4I c) {
		color = c.withAlpha(255);
	}

	public Color4I getColor() {
		return color;
	}

	@Override
	public Color4I getBlockColor(BlockAndTintGetter world, BlockPos pos) {
		return color;
	}
}
