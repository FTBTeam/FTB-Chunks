package com.feed_the_beast.mods.ftbchunks.client.map;

/**
 * @author LatvianModder
 */
public class Waypoint {
	public final MapDimension dimension;
	public boolean hidden = false;
	public int minimapDistance = 4000;
	public int inWorldDistance = 4000;
	public String name = "";
	public int x = 0;
	public int y = -1;
	public int z = 0;
	public int color = 0xFFFFFF;
	public WaypointType type = WaypointType.DEFAULT;

	public double distance;
	public int alpha;

	public Waypoint(MapDimension d) {
		dimension = d;
	}
}