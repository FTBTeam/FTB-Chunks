package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;

/**
 * @author LatvianModder
 */
public class FTBChunksAPIImpl extends FTBChunksAPI
{
	public static ClaimedChunkManagerImpl manager;
	public static final ITag.INamedTag<Block> EDIT_TAG = BlockTags.makeWrapperTag("ftbchunks:edit_whitelist");
	public static final ITag.INamedTag<Block> INTERACT_TAG = BlockTags.makeWrapperTag("ftbchunks:interact_whitelist");

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