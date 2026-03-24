package dev.ftb.mods.ftbchunks.api.fabric;

import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.AddMinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.ChunksUpdatedFromServerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerAvailableEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class FTBChunksClientEvents {
    public static Event<AddMapIconEvent> ADD_MAP_ICON = EventFactory.createArrayBacked(AddMapIconEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    c.accept(data);
                }
            }
    );

    public static Event<WaypointManagerAvailableEvent> WAYPOINT_MANAGER_AVAILABLE = EventFactory.createArrayBacked(WaypointManagerAvailableEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    c.accept(data);
                }
            }
    );

    public static Event<ChunksUpdatedFromServerEvent> CHUNKS_UPDATED_FROM_SERVER = EventFactory.createArrayBacked(ChunksUpdatedFromServerEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    c.accept(data);
                }
            }
    );

    public static Event<AddMinimapLayerEvent> ADD_MINIMAP_LAYER = EventFactory.createArrayBacked(AddMinimapLayerEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    c.accept(data);
                }
            }
    );
}
