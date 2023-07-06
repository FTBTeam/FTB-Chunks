package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.api.client.icon.WaypointIcon;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.mapicon.WaypointMapIcon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class WaypointImpl implements Waypoint {
	private final MapDimension mapDimension;
	private final BlockPos pos;
	private final WaypointType type;

	public double minimapDistance = 50000;
	public double inWorldDistance = 0;

	private boolean hidden = false;
	private String name = "";
	private int color = 0xFFFFFF;
	private WaypointMapIcon mapIcon;

	public WaypointImpl(WaypointType type, MapDimension mapDimension, BlockPos pos) {
		this.type = type;
		this.mapDimension = mapDimension;
		this.pos = pos;
	}

	public WaypointType getType() {
		return type;
	}

	@Override
	public boolean isDeathpoint() {
		return type == WaypointType.DEATH;
	}

	@Override
	public ResourceKey<Level> getDimension() {
		return mapDimension.dimension;
	}

	@Override
	public BlockPos getPos() {
		return pos;
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public WaypointImpl setHidden(boolean hidden) {
		this.hidden = hidden;
		mapDimension.markDirty();
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public WaypointImpl setName(String name) {
		this.name = name;
		mapDimension.markDirty();
		return this;
	}

	@Override
	public int getColor() {
		return color;
	}

	@Override
	public WaypointImpl setColor(int color) {
		this.color = color;
		mapDimension.markDirty();
		return this;
	}

	@Override
	public double getDistanceSq(Entity entity) {
		return entity.distanceToSqr(Vec3.atCenterOf(pos));
	}

	@Override
	public WaypointIcon getMapIcon() {
		return mapIcon;
	}

	public void refreshIcon() {
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

	public void removeFromManager() {
		mapDimension.getWaypointManager().remove(this);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WaypointImpl waypoint = (WaypointImpl) o;
		return pos.equals(waypoint.pos) && mapDimension.dimension.equals(waypoint.mapDimension.dimension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mapDimension.dimension, pos);
	}
}