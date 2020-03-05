package com.feed_the_beast.mods.ftbchunks.net;

/**
 * @author LatvianModder
 */
public class NetClaimedChunk
{
	public int x, z, borders;
	public NetClaimedChunkGroup group;

	@Override
	public String toString()
	{
		return "[" + String.join(",", Integer.toString(x), Integer.toString(z), Integer.toString(borders), Integer.toString(group.id)) + "]";
	}
}