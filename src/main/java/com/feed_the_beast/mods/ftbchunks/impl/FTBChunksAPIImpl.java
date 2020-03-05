package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;

/**
 * @author LatvianModder
 */
public class FTBChunksAPIImpl extends FTBChunksAPI
{
	public static ClaimedChunkManagerImpl manager;

	@Override
	public ClaimedChunkManagerImpl getManager()
	{
		if (manager == null)
		{
			throw new NullPointerException("FTB Chunks Manager hasn't been loaded yet!");
		}

		return manager;
	}
}