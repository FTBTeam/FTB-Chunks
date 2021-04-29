package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface BlockColor {
	Color4I getBlockColor(BlockAndTintGetter world, BlockPos pos);

	default boolean isIgnored() {
		return false;
	}
}
