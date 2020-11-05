package com.feed_the_beast.mods.ftbchunks.client.map.color;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

/**
 * @author LatvianModder
 */
public class CustomBlockColor implements BlockColor
{
	public final Color4I color;

	public CustomBlockColor(Color4I c)
	{
		color = c.withAlpha(255);
	}

	@Override
	public Color4I getBlockColor(IBlockDisplayReader world, BlockPos pos)
	{
		return color;
	}
}
