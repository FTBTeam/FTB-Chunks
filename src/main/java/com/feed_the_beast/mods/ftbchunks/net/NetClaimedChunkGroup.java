package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.util.text.ITextComponent;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class NetClaimedChunkGroup
{
	public int id;
	public int color;
	public boolean forceLoaded;
	public ITextComponent owner;

	@Override
	public int hashCode()
	{
		return Objects.hash(color, forceLoaded, owner);
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == this)
		{
			return true;
		}
		else if (o instanceof NetClaimedChunkGroup)
		{
			NetClaimedChunkGroup g = (NetClaimedChunkGroup) o;
			return color == g.color && forceLoaded == g.forceLoaded && Objects.equals(owner, g.owner);
		}

		return false;
	}

	@Override
	public String toString()
	{
		return "[" + String.join(",", Integer.toString(id), Integer.toHexString(color), owner.getString()) + "]";
	}

	public boolean connect(NetClaimedChunkGroup g)
	{
		return color == g.color && Objects.equals(owner, g.owner);
	}
}