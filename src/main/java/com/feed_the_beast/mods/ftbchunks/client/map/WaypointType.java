package com.feed_the_beast.mods.ftbchunks.client.map;

import net.minecraft.util.ResourceLocation;

/**
 * @author LatvianModder
 */
public enum WaypointType
{
	DEFAULT(new ResourceLocation("ftbchunks:textures/waypoint_default.png")),
	DEATH(new ResourceLocation("ftbchunks:textures/waypoint_death.png")),
	SPAWN(new ResourceLocation("ftbchunks:textures/waypoint_spawn.png")),
	HOME(new ResourceLocation("ftbchunks:textures/waypoint_home.png"));

	public static final WaypointType[] VALUES = values();

	public final ResourceLocation texture;

	WaypointType(ResourceLocation t)
	{
		texture = t;
	}
}