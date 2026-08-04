package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.MapColor;
import org.jspecify.annotations.Nullable;

public record CustomBlockColor(Color4I color) implements BlockColor {
	public static final CustomBlockColor FLOWER_POT = new CustomBlockColor(Color4I.rgb(0x683A2D));
	public static final CustomBlockColor RAIL = new CustomBlockColor(Color4I.rgb(0x888888));
	public static final CustomBlockColor EMPTY = new CustomBlockColor(Color4I.BLACK);

	public CustomBlockColor(Color4I color) {
		this.color = color.withAlpha(255);
	}

	public static CustomBlockColor ofMapColor(@Nullable MapColor color) {
		// note: color *shouldn't* be null here, but see https://github.com/FTBTeam/FTB-Mods-Issues/issues/2168
		return color == null ? EMPTY : new CustomBlockColor(Color4I.rgb(color.col));
	}

	@Override
	public Color4I getBlockColor(BlockAndTintGetter blockAndTintGetter, BlockPos pos) {
		return color;
	}
}
