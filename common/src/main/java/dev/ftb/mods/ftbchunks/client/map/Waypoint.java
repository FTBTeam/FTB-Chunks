package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.mapicon.WaypointMapIcon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class Waypoint {
	public final MapDimension dimension;
	private final BlockPos pos;
	private final WaypointType type;

	public double minimapDistance = 50000;
	public double inWorldDistance = 0;

	private boolean hidden = false;
	private String name = "";
	private int color = 0xFFFFFF;
	private WaypointMapIcon mapIcon;

	public Waypoint(WaypointType type, MapDimension dimension, BlockPos pos) {
		this.type = type;
		this.dimension = dimension;
		this.pos = pos;
	}

	public WaypointType getType() {
		return type;
	}

	public BlockPos getPos() {
		return pos;
	}

	public boolean isHidden() {
		return hidden;
	}

	public Waypoint setHidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	public String getName() {
		return name;
	}

	public Waypoint setName(String name) {
		this.name = name;
		return this;
	}

	public int getColor() {
		return color;
	}

	public Waypoint setColor(int color) {
		this.color = color;
		return this;
	}

	public WaypointMapIcon getMapIcon() {
		return mapIcon;
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

	public double getDistanceSq(Entity entity) {
		return entity.distanceToSqr(Vec3.atCenterOf(pos));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Waypoint waypoint = (Waypoint) o;
		return pos.equals(waypoint.pos) && dimension.dimension.equals(waypoint.dimension.dimension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dimension.dimension, pos);
	}
}