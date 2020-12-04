package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColor;
import com.feed_the_beast.mods.ftbchunks.core.BlockFTBC;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
@Mixin(Block.class)
public abstract class BlockMixin implements BlockFTBC
{
	private BlockColor ftbcBlockColor = null;
	private int ftbcBlockColorIndex = -1;

	@Override
	public void setFTBCBlockColor(@Nullable BlockColor c)
	{
		ftbcBlockColor = c;
	}

	@Override
	@Nullable
	public BlockColor getFTBCBlockColor()
	{
		return ftbcBlockColor;
	}

	@Override
	public int getFTBCBlockColorIndex()
	{
		return ftbcBlockColorIndex;
	}

	@Override
	public void setFTBCBlockColorIndex(int c)
	{
		ftbcBlockColorIndex = c;
	}
}
