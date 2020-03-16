package com.feed_the_beast.mods.ftbchunks.impl;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class KnownFakePlayer
{
	public final UUID uuid;
	public final String name;
	public boolean banned;

	public KnownFakePlayer(UUID id, String n, boolean b)
	{
		uuid = id;
		name = n;
		banned = b;
	}
}