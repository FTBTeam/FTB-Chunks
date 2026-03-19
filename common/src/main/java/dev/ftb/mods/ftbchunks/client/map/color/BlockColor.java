package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;

@FunctionalInterface
public interface BlockColor {
	Color4I getBlockColor(BlockAndTintGetter world, BlockPos pos);

	default boolean isIgnored() {
		return false;
	}
}
