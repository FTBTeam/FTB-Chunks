package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class FTBChunksAPIImpl extends FTBChunksAPI
{
	public static ClaimedChunkManagerImpl manager;
	public static final Tag<Block> EDIT_TAG = new BlockTags.Wrapper(new ResourceLocation("ftbchunks", "edit_whitelist"));
	public static final Tag<Block> INTERACT_TAG = new BlockTags.Wrapper(new ResourceLocation("ftbchunks", "interact_whitelist"));
	public static final Tag<Block> MAP_IGNORE_IN_WATER_TAG = new BlockTags.Wrapper(new ResourceLocation("ftbchunks", "map_ignore_in_water"));
	public static Map<Block, Color4I> COLOR_MAP = new HashMap<>();

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