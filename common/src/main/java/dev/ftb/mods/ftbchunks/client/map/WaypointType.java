package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftblibrary.icon.Icon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WaypointType {
	private static final Map<String, WaypointType> TYPES = new ConcurrentHashMap<>();

	public static final WaypointType DEFAULT
			= WaypointType.builder().withOutsideIcon("ftbchunks:textures/waypoint_default_outside.png").canChangeColor().build("default");
	public static final WaypointType DEATH
			= WaypointType.builder().build("death");

	private final String id;
	private final Icon icon;
	private final Icon outsideIcon;
	private final boolean canChangeColor;

	public static Builder builder() {
		return new Builder();
	}

	public static WaypointType forId(String typeName) {
		return TYPES.getOrDefault(typeName, WaypointType.DEFAULT);
	}

	private WaypointType(String id, Builder builder) {
		this.id = id;

		icon = Icon.getIcon("ftbchunks:textures/waypoint_" + this.id + ".png");
		outsideIcon = builder.outsideIcon == null ? icon : builder.outsideIcon;
		canChangeColor = builder.canChangeColor;
	}

	public String getId() {
		return id;
	}

	public Icon getOutsideIcon() {
		return outsideIcon;
	}

	public boolean canChangeColor() {
		return canChangeColor;
	}

	public Icon getIcon() {
		return icon;
	}

	public static class Builder {
		private Icon outsideIcon = null;
		private boolean canChangeColor = false;

		public Builder withOutsideIcon(String icon) {
			outsideIcon = Icon.getIcon(icon);
			return this;
		}

		public Builder withOutsideIcon(Icon icon) {
			outsideIcon = icon;
			return this;
		}

		public Builder canChangeColor() {
			canChangeColor = true;
			return this;
		}

		public WaypointType build(String id) {
			WaypointType type = new WaypointType(id, this);
			TYPES.put(id, type);
			return type;
		}
	}
}