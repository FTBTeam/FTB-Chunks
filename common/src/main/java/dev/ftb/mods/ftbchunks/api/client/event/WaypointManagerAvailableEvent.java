package dev.ftb.mods.ftbchunks.api.client.event;

import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;

import java.util.function.Consumer;

/// Fired when the [WaypointManager] becomes available, immediately after the client player enters a new
/// dimension, including initial login.
///
/// Corresponding platform-native events to listen to:
/// * `FTBChunksClientEvent.WaypointManagerAvailable` (NeoForge)
/// * `FTBChunksClientEvents.WAYPOINT_MANAGER_AVAILABLE` (Fabric)
@FunctionalInterface
public interface WaypointManagerAvailableEvent extends Consumer<WaypointManagerAvailableEvent.Data> {
    record Data(WaypointManager manager) {
    }
}
