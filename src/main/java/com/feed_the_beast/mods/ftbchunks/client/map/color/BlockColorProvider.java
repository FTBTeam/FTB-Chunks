package com.feed_the_beast.mods.ftbchunks.client.map.color;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public interface BlockColorProvider
{
	void setFTBCBlockColor(@Nullable BlockColor c);

	BlockColor getFTBCBlockColor();
}
