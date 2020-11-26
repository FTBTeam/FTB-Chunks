package com.feed_the_beast.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

/**
 * @author LatvianModder
 */
public class IgnoredBlockColor implements BlockColor
{
	@Override
	public Color4I getBlockColor(IBlockDisplayReader world, BlockPos pos)
	{
		return pos.getY() == 0 ? Color4I.BLACK : Icon.EMPTY;
	}

	@Override
	public boolean isIgnored()
	{
		return true;
	}
}
