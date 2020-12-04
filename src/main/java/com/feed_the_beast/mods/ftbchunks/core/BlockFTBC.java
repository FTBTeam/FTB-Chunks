package com.feed_the_beast.mods.ftbchunks.core;

import com.feed_the_beast.mods.ftbchunks.client.map.color.BlockColor;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public interface BlockFTBC
{
	void setFTBCBlockColor(@Nullable BlockColor c);

	@Nullable
	BlockColor getFTBCBlockColor();

	void setFTBCBlockColorIndex(int c);

	int getFTBCBlockColorIndex();
}
