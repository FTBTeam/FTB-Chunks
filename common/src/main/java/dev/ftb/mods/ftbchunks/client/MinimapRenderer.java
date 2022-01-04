package dev.ftb.mods.ftbchunks.client;

import net.minecraft.resources.ResourceLocation;

public interface MinimapRenderer {

	/**
	 * @param x           position in-world
	 * @param z           position in-world
	 * @param color       the icon tint
	 * @param maxDistance maximum distance from the player the icon should be show.
	 *                    Set to 0 to disable rendering on the edge if it would be off-map.
	 *                    Set to a negative number for infinity.
	 */
	void render(double x, double z, int color, int maxDistance, ResourceLocation texture);
}
