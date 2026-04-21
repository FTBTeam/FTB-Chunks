package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockColor {
	Color4I getBlockColor(BlockAndTintGetter blockAndTintGetter, BlockPos pos);

	default boolean isIgnored(Level level, BlockPos pos, BlockState state) {
		return false;
	}
}
