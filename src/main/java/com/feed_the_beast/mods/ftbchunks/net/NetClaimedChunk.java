package com.feed_the_beast.mods.ftbchunks.net;

import java.util.Date;

/**
 * @author LatvianModder
 */
public class NetClaimedChunk
{
	public int x, z, borders;
	public NetClaimedChunkGroup group;
	public long relativeTimeClaimed;
	public long relativeTimeForceLoaded;
	public Date claimedDate;
	public Date forceLoadedDate;

	@Override
	public String toString()
	{
		return "[" + String.join(",", Integer.toString(x), Integer.toString(z), Integer.toString(group.id)) + "]";
	}
}