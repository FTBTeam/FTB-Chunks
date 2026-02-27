package dev.ftb.mods.ftbchunks.api.client.waypoint;

import dev.ftb.mods.ftbchunks.api.client.icon.WaypointIcon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Optional;

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
     * Get the waypoint's raw name, which the player entered, and which is displayed in editing GUIs. This may be a
     * translation key, or just a literal string.
     *
     * @return the waypoint's name
     */
    String getName();

    /**
     * {@return the displayed string for the waypoint, as shown on the large map, and for in-world waypoints}
     */
    default Component getDisplayName() {
        return Component.translatable(getName());
    }

    /**
     * Change the waypoint's displayed name.
     *
     * @param name the new name
     * @return the waypoint itself, for fluent calling
     */
    Waypoint setName(String name);

    /**
     * {@return the waypoint's color, in RGB format}
     */
    int getColor();

    /**
     * Change the waypoint's color.
     *
     * @param color the new color, in RGB format
     * @return the waypoint itself, for fluent calling
     */
    Waypoint setColor(int color);

    /**
     * {@return true if this waypoint is a death point, false if a normal waypoint}
     */
    boolean isDeathpoint();

    /**
     * {@return true if this waypoint is transient - not saved across client sessions}
     */
    boolean isTransient();

    /**
     * Set the transient status of this waypoint. Transient waypoints are not saved across client sessions.
     * @param isTransient the new transient status
     * @return the waypoint itself, for fluent calling
     */
    Waypoint setTransient(boolean isTransient);

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
    Optional<WaypointIcon> getMapIcon();
}
