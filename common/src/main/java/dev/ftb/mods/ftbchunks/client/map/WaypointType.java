package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.icon.Icon;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class WaypointType {
	public static final Map<String, WaypointType> TYPES = new HashMap<>();

	public static WaypointType add(String id) {
		WaypointType type = new WaypointType(id);
		TYPES.put(id, type);
		return type;
	}

	public static final WaypointType DEFAULT = add("default").outsideIcon("ftbchunks:textures/waypoint_default_edge.png").canChangeColor();
	public static final WaypointType DEATH = add("death");

	public final String id;
	public Icon icon;
	public Icon outsideIcon;
	public boolean canChangeColor;

	private WaypointType(String i) {
		id = i;
		icon = Icon.getIcon("ftbchunks:textures/waypoint_" + id + ".png");
		outsideIcon = icon;
		canChangeColor = false;
	}

	public WaypointType icon(String t) {
		icon = Icon.getIcon(t);
		return this;
	}

	public WaypointType outsideIcon(String t) {
		outsideIcon = Icon.getIcon(t);
		return this;
	}

	public WaypointType canChangeColor() {
		canChangeColor = true;
		return this;
	}
}