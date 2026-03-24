package dev.ftb.mods.ftbchunks.api.client.event;

import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/// Fired when minimap rendering layers are being registered. You can listen to this event to add custom rendering
/// layers to the minimap.
///
/// Corresponding platform-native events to listen to:
/// * `FTBChunksClientEvent.AddMinimapLayer` (NeoForge)
/// * `FTBChunksClientEvents.ADD_MINIMAP_LAYER` (Fabric)
public interface AddMinimapLayerEvent extends Consumer<AddMinimapLayerEvent.Data> {
    record Data(Consumer<PositionedLayer> callback) {
        /// Add a new rendering layer to the minimap.
        ///
        /// @param id       a unique ID for this layer, in your mod's namespace
        /// @param renderer the renderer object
        /// @param order    the order of this renderer
        public void addLayer(Identifier id, MinimapLayerRenderer renderer, Order order) {
            callback.accept(new PositionedLayer(id, renderer, order));
        }

        /// Add a new rendering layer to the minimap, at the end of the list (i.e. will render last, above all other layers)
        ///
        /// @param id       a unique ID for this layer, in your mod's namespace
        /// @param renderer the renderer object
        public void addLayer(Identifier id, MinimapLayerRenderer renderer) {
            addLayer(id, renderer, Order.atEnd());
        }
    }

    /// Defines how a layer should be ordered relative to other layers in the map. See also
    /// [dev.ftb.mods.ftbchunks.api.client.minimap.DefaultRenderLayers] for a collection of the layer IDs which
    /// are always added by FTB Chunks itself.
    ///
    /// @param after should the layer go before or after the other layer?
    /// @param otherLayer a layer ID, or null to indicate adding at the start or end of the layer list
    record Order(boolean after, @Nullable Identifier otherLayer) {
        public static Order atEnd() {
            return new Order(true, null);
        }

        public static Order atStart() {
            return new Order(false, null);
        }

        public static Order before(Identifier otherLayer) {
            return new Order(false, otherLayer);
        }

        public static Order after(Identifier otherLayer) {
            return new Order(true, otherLayer);
        }
    }

    record PositionedLayer(Identifier id, MinimapLayerRenderer renderer, Order order) {
    }
}
