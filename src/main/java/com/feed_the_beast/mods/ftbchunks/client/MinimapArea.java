package com.feed_the_beast.mods.ftbchunks.client;

import net.minecraft.util.math.ChunkPos;

/**
 * @author LatvianModder
 */
public class MinimapArea
{
	public final MinimapDimension dim;
	public final ChunkPos pos;

	public MinimapArea(MinimapDimension d, ChunkPos p)
	{
		dim = d;
		pos = p;
	}
}