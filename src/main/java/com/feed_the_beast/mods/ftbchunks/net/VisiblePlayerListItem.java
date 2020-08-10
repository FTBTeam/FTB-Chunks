package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkPlayerDataImpl;
import com.feed_the_beast.mods.ftbchunks.impl.PlayerLocation;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class VisiblePlayerListItem
{
	public ServerPlayerEntity player;
	public ClaimedChunkPlayerDataImpl data;
	public PlayerLocation location;

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		VisiblePlayerListItem that = (VisiblePlayerListItem) o;
		return location.equals(that.location);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(location);
	}
}
