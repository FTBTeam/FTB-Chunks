package dev.ftb.mods.ftbchunks.api.neoforge;

import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.AddMinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.ChunksUpdatedFromServerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerAvailableEvent;
import dev.ftb.mods.ftblibrary.api.neoforge.BaseEventWithData;

public class FTBChunksClientEvent {
    public static class MapIcon extends BaseEventWithData<AddMapIconEvent.Data> {
        public MapIcon(AddMapIconEvent.Data data) {
            super(data);
        }
    }

    public static class WaypointManagerAvailable extends BaseEventWithData<WaypointManagerAvailableEvent.Data> {
        public WaypointManagerAvailable(WaypointManagerAvailableEvent.Data data) {
            super(data);
        }
    }

    public static class AddMinimapLayer extends BaseEventWithData<AddMinimapLayerEvent.Data> {
        public AddMinimapLayer(AddMinimapLayerEvent.Data data) {
            super(data);
        }
    }

    public static class ChunksUpdatedFromServer extends BaseEventWithData<ChunksUpdatedFromServerEvent.Data> {
        public ChunksUpdatedFromServer(ChunksUpdatedFromServerEvent.Data data) {
            super(data);
        }
    }
}
