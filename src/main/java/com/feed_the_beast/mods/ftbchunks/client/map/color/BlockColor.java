package com.feed_the_beast.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface BlockColor
{
	Color4I getBlockColor(BlockState state, IBlockDisplayReader world, BlockPos pos);

	default boolean isIgnored()
	{
		return false;
	}
}
