package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.world.dimension.DimensionType;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class Waypoint
{
	public UUID id = UUID.randomUUID();
	public String name = "";
	public String owner = "";
	public int x = 0, y = 0, z = 0;
	public DimensionType dimension = DimensionType.OVERWORLD;
	public PrivacyMode mode = PrivacyMode.PRIVATE;
	public int color = 0xFFFFFF;
	public WaypointType type = WaypointType.DEFAULT;
}