package dev.ftb.mods.ftbchunks.api.client.waypoint;

import dev.ftb.mods.ftbchunks.api.client.icon.WaypointIcon;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * A waypoint; these can be displayed on the minimap, the large fullscreen map, or as in-world icons. Waypoints can be
 * added by players, or automatically added on player death.
 * <p>
 * See also {@link WaypointManager} for methods to add/delete/retrieve waypoints.
 */
public interface Waypoint {
    /**
     * Get the dimension this waypoint is in.
     *
     * @return the dimension
     */
    ResourceKey<Level> getDimension();

    /**
     * Get the waypoint's position.
     *
     * @return the position
     */
    BlockPos getPos();

    /**
     * Is this waypoint currently hidden?
     *
     * @return true if the waypoint is hidden
     */
    boolean isHidden();

    /**
     * Mark the waypoint as hidden or otherwise.
     *
     * @param hidden whether to hide the waypoint
     * @return the waypoint itself, for fluent calling
     */
    Waypoint setHidden(boolean hidden);

    /**
     * Get the waypoint's displayed name.
     *
     * @return the name
     */
    String getName();

    /**
     * Change the waypoint's displayed name.
     *
     * @param name the new name
     * @return the waypoint itself, for fluent calling
     */
    Waypoint setName(String name);

    /**
     * Get the waypoint's color, in RGBA format
     *
     * @return the color
     */
    int getColor();

    /**
     * Change the waypoint's color.
     *
     * @param color the new color
     * @return the waypoint itself, for fluent calling
     */
    Waypoint setColor(int color);

    /**
     * Is this a player death point?
     *
     * @return true for a death point, false for a normal waypoint
     */
    boolean isDeathpoint();

    /**
     * Get the squared distance from the waypoint to the given entity (typically the client player).
     *
     * @param entity the entity to check
     * @return the squared distance
     */
    double getDistanceSq(Entity entity);

    /**
     * Get the map icon used to render this waypoint.
     *
     * @return the map icon
     */
    WaypointIcon getMapIcon();
}
