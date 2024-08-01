package dev.ftb.mods.ftbchunks.api.client;

import com.google.common.collect.ImmutableList;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
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

    /**
     * Register a custom minimap info component {@link MinimapInfoComponent} to be rendered on the minimap.
     *
     * This should be called during mod initialization as this list will be finalized once Minecraft has "started"
     * per the client lifecycle events
     *
     * @param component the component to register
     */
    void registerMinimapComponent(MinimapInfoComponent component);


    /**
     * @param component the component to check if it is enabled
     * @return is the component enabled and will render under the minimap
     */
    boolean isMinimapComponentEnabled(MinimapInfoComponent component);

    /**
     * Enable or disable a specific minimap component for rendering under the minimap
     * @param component the component to enable or disable to that shoul
     * @param enabled  the state to set the component to
     */
    void setMinimapComponentEnabled(MinimapInfoComponent component, boolean enabled);

    /**
     * Provides an immutable list of all registered minimap components.
     */
    ImmutableList<MinimapInfoComponent> getMinimapComponents();
}
