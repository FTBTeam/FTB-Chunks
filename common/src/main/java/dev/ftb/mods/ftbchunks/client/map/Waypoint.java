package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.client.WaypointMapIcon;

/**
 * @author LatvianModder
 */
public class Waypoint {
	public final MapDimension dimension;
	public boolean hidden = false;
	public double minimapDistance = 50000;
	public double inWorldDistance = 4000;
	public String name = "";
	public int x = 0;
	public int y = 0;
	public int z = 0;
	public int color = 0xFFFFFF;
	public WaypointType type = WaypointType.DEFAULT;

	public WaypointMapIcon mapIcon;

	public Waypoint(MapDimension d) {
		dimension = d;
	}

	public void update() {
		mapIcon = new WaypointMapIcon(this);
	}
}