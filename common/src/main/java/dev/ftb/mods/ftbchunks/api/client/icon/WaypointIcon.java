package dev.ftb.mods.ftbchunks.api.client.icon;

import dev.ftb.mods.ftblibrary.icon.Color4I;

/**
 * Adds a few waypoint-specific methods to the base map icon interface.
 */
public interface WaypointIcon extends MapIcon {
    /**
     * Get the current rendering alpha for the map icon. This is generally automatically set based on the player's
     * proximity to the icon.
     *
     * @return the current alpha (0..255)
     */
    int getAlpha();

    /**
     * Set the rendering alpha for the map icon. This is generally automatically set based on the player's proximity
     * to the icon.
     *
     * @param alpha the current alpha (0..255)
     */
    void setAlpha(int alpha);

    /**
     * Get the rendering color for the icon. This is just a convenience to get the color of the waypoint that this
     * icon represents.
     *
     * @return the waypoint/icon's color
     */
    Color4I getColor();
}
