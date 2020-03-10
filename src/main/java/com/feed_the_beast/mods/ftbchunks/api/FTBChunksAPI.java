package com.feed_the_beast.mods.ftbchunks.api;

import com.feed_the_beast.mods.ftbchunks.ClaimedChunkManager;

/**
 * @author LatvianModder
 */
public abstract class FTBChunksAPI
{
	public static FTBChunksAPI INSTANCE;

	public abstract ClaimedChunkManager getManager();
}