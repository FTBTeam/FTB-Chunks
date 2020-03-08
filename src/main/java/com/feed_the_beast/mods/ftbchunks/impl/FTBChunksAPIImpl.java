package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class FTBChunksAPIImpl extends FTBChunksAPI
{
	public static ClaimedChunkManagerImpl manager;
	private static final HashSet<Block> BLOCK_EDIT_WHITELIST = new HashSet<>();
	private static final HashSet<Block> BLOCK_INTERACT_WHITELIST = new HashSet<>();

	@Override
	public ClaimedChunkManagerImpl getManager()
	{
		if (manager == null)
		{
			throw new NullPointerException("FTB Chunks Manager hasn't been loaded yet!");
		}

		return manager;
	}

	@Override
	public Set<Block> getEditWhitelist()
	{
		return BLOCK_EDIT_WHITELIST;
	}

	@Override
	public Set<Block> getInteractWhitelist()
	{
		return BLOCK_INTERACT_WHITELIST;
	}
}