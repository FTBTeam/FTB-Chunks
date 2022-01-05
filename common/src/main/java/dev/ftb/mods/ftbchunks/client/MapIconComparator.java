package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.integration.MapIcon;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class MapIconComparator implements Comparator<MapIcon> {
	public final Vec3 pos;
	public final float delta;

	public MapIconComparator(Vec3 p, float d) {
		pos = p;
		delta = d;
	}

	@Override
	public int compare(MapIcon o1, MapIcon o2) {
		int i = Integer.compare(o1.getPriority(), o2.getPriority());
		return i == 0 ? Double.compare(o2.getPos(delta).distanceToSqr(pos), o1.getPos(delta).distanceToSqr(pos)) : i;
	}
}
