package com.feed_the_beast.mods.ftbchunks.api;

import com.feed_the_beast.mods.ftbchunks.ClaimedChunkManager;
import net.minecraft.block.Block;

import java.util.Set;

/**
 * @author LatvianModder
 */
public abstract class FTBChunksAPI
{
	public static FTBChunksAPI INSTANCE;

	public abstract ClaimedChunkManager getManager();

	public abstract Set<Block> getEditWhitelist();

	public abstract Set<Block> getInteractWhitelist();
}