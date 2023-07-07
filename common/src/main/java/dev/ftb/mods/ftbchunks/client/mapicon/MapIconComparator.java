package dev.ftb.mods.ftbchunks.client.mapicon;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class MapIconComparator implements Comparator<MapIcon> {
	private final Vec3 pos;
	private final float delta;

	public MapIconComparator(Vec3 pos, float delta) {
		this.pos = pos;
		this.delta = delta;
	}

	@Override
	public int compare(MapIcon o1, MapIcon o2) {
		int i = Integer.compare(o1.getPriority(), o2.getPriority());
		return i == 0 ? Double.compare(o2.getPos(delta).distanceToSqr(pos), o1.getPos(delta).distanceToSqr(pos)) : i;
	}
}
