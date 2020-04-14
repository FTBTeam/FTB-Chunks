package com.feed_the_beast.mods.ftbchunks.impl.map;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author LatvianModder
 */
public interface MapColorGetter
{
	int getColor(World world, BlockPos p);
}