package dev.ftb.mods.ftbchunks.integration;

public class InWorldMapIcon {
	public final MapIcon icon;
	public final float x;
	public final float y;
	public boolean isMouseOver;
	public final double distanceToPlayer;
	public final double distanceToMouse;

	public InWorldMapIcon(MapIcon icon, float x, float y, double distanceToPlayer, double distanceToMouse) {
		this.icon = icon;
		this.x = x;
		this.y = y;
		this.isMouseOver = false;
		this.distanceToPlayer = distanceToPlayer;
		this.distanceToMouse = distanceToMouse;
	}
}
