package com.feed_the_beast.mods.ftbchunks.impl.map;

/**
 * @author LatvianModder
 */
public interface MapTask extends Runnable
{
	default boolean cancelOtherTasks()
	{
		return false;
	}
}