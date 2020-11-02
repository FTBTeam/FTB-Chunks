package com.feed_the_beast.mods.ftbchunks.mixin;

import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColor;
import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColorProvider;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

/**
 * @author LatvianModder
 */
@Mixin(Block.class)
public abstract class ClientBlockMixin implements BlockColorProvider
{
	public BlockColor ftbcBlockColor;

	@Override
	public void setFTBCBlockColor(BlockColor c)
	{
		ftbcBlockColor = c;
	}

	@Override
	public BlockColor getFTBCBlockColor()
	{
		return ftbcBlockColor;
	}
}
