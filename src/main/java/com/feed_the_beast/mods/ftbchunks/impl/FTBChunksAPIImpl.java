package com.feed_the_beast.mods.ftbchunks.impl;

import com.feed_the_beast.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

/**
 * @author LatvianModder
 */
public class FTBChunksAPIImpl extends FTBChunksAPI {
	public static ClaimedChunkManagerImpl manager;
	public static final Tags.IOptionalNamedTag<Block> EDIT_TAG = BlockTags.createOptional(new ResourceLocation("ftbchunks", "edit_whitelist"));
	public static final Tags.IOptionalNamedTag<Block> INTERACT_TAG = BlockTags.createOptional(new ResourceLocation("ftbchunks", "interact_whitelist"));

	@Override
	public ClaimedChunkManagerImpl getManager() {
		if (manager == null) {
			throw new NullPointerException("FTB Chunks Manager hasn't been loaded yet!");
		}

		return manager;
	}
}