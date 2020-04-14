package com.feed_the_beast.mods.ftbchunks.impl;

import net.minecraft.world.dimension.DimensionType;

/**
 * @author LatvianModder
 */
public class Waypoint
{
	public final ClaimedChunkPlayerDataImpl playerData;
	public DimensionType dimension;
	public double x, y, z;
	public int color;
	public boolean isPublic;

	public Waypoint(ClaimedChunkPlayerDataImpl p)
	{
		playerData = p;
		dimension = DimensionType.OVERWORLD;
		x = y = z = 0D;
		color = 0xFFFFFF;
		isPublic = false;
	}
}