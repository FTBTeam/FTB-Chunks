package dev.ftb.mods.ftbchunks.api.client.waypoint;

import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Optional;

/**
 * Allows access to the waypoints for a particular dimension. A waypoint manager is dimension-specific; an instance
 * of the manager for a dimension can be obtained via {@link FTBChunksClientAPI#getWaypointManager()} or
 * {@link FTBChunksClientAPI#getWaypointManager(ResourceKey)}.
 */
public interface WaypointManager {
    /**
     * Add a new waypoint at the given block position, with the given name. The returned Waypoint object can be used
     * to alter other properties of the waypoint (hidden, color, etc.).
     * <p>
     * If a waypoint already exists at the given position, a new waypoint will not be added.
     *
     * @param pos the position to add the waypoint at
     * @param name the waypoint's displayed name
     * @return a newly-added waypoint
     */
    Waypoint addWaypointAt(BlockPos pos, String name);

    /**
     * Remove the waypoint at the given position from the manager, if there is one.
     *
     * @param pos the position at which to
     * @return true if the waypoint was present
     */
    boolean removeWaypointAt(BlockPos pos);

    /**
     * Remove the given waypoint from the manager, if it is present.
     *
     * @param waypoint the waypoint to remove
     * @return true if the waypoint was present
     */
    boolean removeWaypoint(Waypoint waypoint);

    /**
     * Get an unmodifiable view of all known waypoints in this waypoint manager. Waypoints within this collection
     * can be modified, but you can not use this method to add or remove waypoints.
     *
     * @return all known waypoints
     */
    Collection<Waypoint> getAllWaypoints();

    /**
     * Get the death point closes to the given player (typically the client player), if any.
     *
     * @param player the player to check
     * @return the closest death point, or {@code Optional.empty()} if there are no deathpoints currently
     */
    Optional<? extends Waypoint> getNearestDeathpoint(Player player);
}
