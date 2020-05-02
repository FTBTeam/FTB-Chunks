package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.world.dimension.DimensionType;

/**
 * @author LatvianModder
 */
public class Waypoint
{
	public String name = "";
	public int x = 0, y = 0, z = 0;
	public DimensionType dimension = DimensionType.OVERWORLD;
	public PrivacyMode mode = PrivacyMode.PRIVATE;
	public int color = 0xFFFFFF;
	public WaypointType type = WaypointType.DEFAULT;
}