package com.feed_the_beast.mods.ftbchunks.api;

/**
 * @author LatvianModder
 */
public interface ClaimResult
{
	default boolean isSuccess()
	{
		return false;
	}
}