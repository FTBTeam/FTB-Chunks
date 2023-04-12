package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.WaypointMapIcon;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class Waypoint {
	public final MapDimension dimension;
	public final int x;
	public final int y;
	public final int z;
	public boolean hidden = false;
	public double minimapDistance = 50000;
	public double inWorldDistance = 0;
	public String name = "";
	public int color = 0xFFFFFF;
	public WaypointType type = WaypointType.DEFAULT;

	public WaypointMapIcon mapIcon;

	public Waypoint(MapDimension d, int x, int y, int z) {
		dimension = d;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void update() {
		mapIcon = new WaypointMapIcon(this);
	}

	public double getDrawDistance(boolean isMinimap) {
		// TODO allow draw distances to be configured per-waypoint
		if (isMinimap) {
			return minimapDistance;
		} else {
			return inWorldDistance == 0 ? FTBChunksClientConfig.WAYPOINT_MAX_DISTANCE.get() : inWorldDistance;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Waypoint waypoint = (Waypoint) o;
		return x == waypoint.x && y == waypoint.y && z == waypoint.z && dimension.dimension.equals(waypoint.dimension.dimension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dimension.dimension, x, y, z);
	}
}