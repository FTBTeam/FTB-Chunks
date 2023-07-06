package dev.ftb.mods.ftbchunks.api.client;

import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface FTBChunksClientAPI {
    /**
     * Get the waypoint manager for the currently-displayed dimension, if possible.
     *
     * @return the current waypoint manager
     */
    Optional<WaypointManager> getWaypointManager();

    /**
     * Get the waypoint manager for a specific dimension, if possible.
     *
     * @return the waypoint manager for the given dimension
     */
    Optional<WaypointManager> getWaypointManager(ResourceKey<Level> dimension);

    /**
     * Schedule an immediate (next tick) refresh of minimap icons. While minimap icons are periodically refreshed in
     * any case, this may be useful to call if you made a change that requires a quicker update (e.g. spawned one or more
     * entities...)
     */
    void requestMinimapIconRefresh();
}
