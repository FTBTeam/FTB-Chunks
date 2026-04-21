package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.api.client.event.AddMinimapLayerEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

/// Represents a layer of rendering to be added to the FTB Chunks minimap. Implementations of this can be registered
/// by listening to [AddMinimapLayerEvent]
@FunctionalInterface
public interface MinimapLayerRenderer {
    /// Render the layer. When this method is called, the active translation has (0,0) at the center of the minimap.
    ///
    /// @param graphics the graphics context
    /// @param poseStack the current pose
    /// @param ctx the minimap context
    void extractLayer(GuiGraphicsExtractor graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx);

    /// {@return true if this layer should be rendered at this time, false if not}
    default boolean shouldExtract(MinimapRenderContext ctx) {
        return true;
    }
}
