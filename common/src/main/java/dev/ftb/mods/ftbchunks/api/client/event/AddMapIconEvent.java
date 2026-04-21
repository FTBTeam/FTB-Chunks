package dev.ftb.mods.ftbchunks.api.client.event;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/// Fired when FTB Chunks is gathering icons to add to the minimap. Handlers for this event should run quickly,
/// since it's called quite frequently to keep minimap icons up-to-date, and a slow handler can negatively impact
/// client FPS.
///
/// Corresponding platform-native events to listen to:
/// * `FTBChunksClientEvent.AddMapIcon` (NeoForge)
/// * `FTBChunksClientEvents.ADD_MAP_ICON` (Fabric)
@FunctionalInterface
public interface AddMapIconEvent extends Consumer<AddMapIconEvent.Data> {
    record Data(ResourceKey<Level> dimension, Consumer<MapIcon> iconConsumer, MapType mapType) {
        /// Add a map icon to the map.
        ///
        /// @param mapIcon the icon to add
        public void add(MapIcon mapIcon) {
            iconConsumer.accept(mapIcon);
        }
    }
}
