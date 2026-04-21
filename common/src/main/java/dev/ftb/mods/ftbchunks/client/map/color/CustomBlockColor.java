package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.MapColor;

public record CustomBlockColor(Color4I color) implements BlockColor {
	public static final CustomBlockColor FLOWER_POT = new CustomBlockColor(Color4I.rgb(0x683A2D));
	public static final CustomBlockColor RAIL = new CustomBlockColor(Color4I.rgb(0x888888));

	public CustomBlockColor(Color4I color) {
		this.color = color.withAlpha(255);
	}

	public static CustomBlockColor ofMapColor(MapColor color) {
		return new CustomBlockColor(Color4I.rgb(color.col));
	}

	@Override
	public Color4I getBlockColor(BlockAndTintGetter blockAndTintGetter, BlockPos pos) {
		return color;
	}
}
