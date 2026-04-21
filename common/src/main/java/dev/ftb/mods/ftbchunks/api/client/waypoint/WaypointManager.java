package dev.ftb.mods.ftbchunks.api.client.waypoint;

import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerAvailableEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Optional;

/// Allows access to the waypoints for a particular dimension. A waypoint manager is dimension-specific; an instance
/// of the manager for a dimension can be obtained via [FTBChunksClientAPI#getWaypointManager()],
/// [FTBChunksClientAPI#getWaypointManager(ResourceKey)], or by listening to the [WaypointManagerAvailableEvent].
public interface WaypointManager {
    /// Add a new waypoint at the given block position, with the given name. The returned Waypoint object can be used
    /// to alter other properties of the waypoint (hidden, color, etc.).
    ///
    /// If a waypoint already exists at the given position, a new waypoint will not be added.
    ///
    /// @param pos the position to add the waypoint at
    /// @param name the waypoint's displayed name
    /// @return a newly-added waypoint
    Waypoint addWaypointAt(BlockPos pos, String name);

    /// Acts like [#addWaypointAt(BlockPos, String)] but waypoints added with this method do not persist across
    /// client sessions; they are forgotten when the player changes dimension or logs out.
    ///
    /// Therefore, this method should be called when the client player enters a dimension, including on initial login;
    /// use the [WaypointManagerAvailableEvent] event to ensure the waypoint manager is definitely available.
    /// Level-joined events fired by Architectury or the current mod loader may be fired too early, before the waypoint
    /// manager is available for the dimension.
    ///
    /// @param pos the position to add the waypoint at
    /// @param name the waypoint's displayed name
    /// @return a newly-added waypoint
    Waypoint addTransientWaypointAt(BlockPos pos, String name);

    /// Remove the waypoint at the given position from the manager, if there is one.
    ///
    /// @param pos the position at which to
    /// @return true if the waypoint was present
    boolean removeWaypointAt(BlockPos pos);

    /// Remove the given waypoint from the manager, if it is present.
    ///
    /// @param waypoint the waypoint to remove
    /// @return true if the waypoint was present
    boolean removeWaypoint(Waypoint waypoint);

    /// Get an unmodifiable view of all known waypoints in this waypoint manager. Waypoints within this collection
    /// can be modified, but you can not use this method to add or remove waypoints.
    ///
    /// @return all known waypoints
    Collection<Waypoint> getAllWaypoints();

    /// Get the death point closest to the given player (typically the client player), if any.
    ///
    /// @param player the player to check
    /// @return the closest death point, or `Optional.empty()` if there are no deathpoints currently
    Optional<? extends Waypoint> getNearestDeathpoint(Player player);
}
